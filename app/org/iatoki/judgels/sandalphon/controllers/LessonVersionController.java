package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.GitCommit;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.VersionCommitForm;
import org.iatoki.judgels.sandalphon.views.html.lesson.version.listVersionsView;
import org.iatoki.judgels.sandalphon.views.html.lesson.version.viewVersionLocalChangesView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class LessonVersionController extends AbstractJudgelsController {

    private final LessonService lessonService;

    @Inject
    public LessonVersionController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result listVersionHistory(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAllowedToViewVersionHistory(lessonService, lesson)) {
            return notFound();
        }

        List<GitCommit> versions = lessonService.getVersions(IdentityUtils.getUserJid(), lesson.getJid());
        boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());
        boolean isAllowedToRestoreVersionHistory = isClean && LessonControllerUtils.isAllowedToRestoreVersionHistory(lessonService, lesson);

        LazyHtml content = new LazyHtml(listVersionsView.render(versions, lesson.getId(), isAllowedToRestoreVersionHistory));
        appendSubtabsLayout(content, lesson);
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.version.history"), routes.LessonVersionController.listVersionHistory(lesson.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Versions - History");

        SandalphonControllerUtils.getInstance().addActivityLog("List version history of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    public Result restoreVersionHistory(long lessonId, String hash) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());

        if (!isClean || !LessonControllerUtils.isAllowedToRestoreVersionHistory(lessonService, lesson)) {
            return notFound();
        }

        lessonService.restore(lesson.getJid(), hash);

        SandalphonControllerUtils.getInstance().addActivityLog("Restore version history " + hash + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonVersionController.listVersionHistory(lesson.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result viewVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            return notFound();
        }

        boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());

        Form<VersionCommitForm> versionCommitForm = Form.form(VersionCommitForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("View version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showViewVersionLocalChanges(versionCommitForm, lesson, isClean);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCommitVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            return notFound();
        }

        Form<VersionCommitForm> versionCommitForm = Form.form(VersionCommitForm.class).bindFromRequest();
        if (formHasErrors(versionCommitForm)) {
            boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());
            return showViewVersionLocalChanges(versionCommitForm, lesson, isClean);
        }

        VersionCommitForm versionCommitData = versionCommitForm.get();

        if (lessonService.fetchUserClone(IdentityUtils.getUserJid(), lesson.getJid())) {
            flash("localChangesError", Messages.get("lesson.version.local.cantCommit"));
        } else if (!lessonService.commitThenMergeUserClone(IdentityUtils.getUserJid(), lesson.getJid(), versionCommitData.title, versionCommitData.description)) {
            flash("localChangesError", Messages.get("lesson.version.local.cantMerge"));
        } else if (!lessonService.pushUserClone(IdentityUtils.getUserJid(), lesson.getJid())) {
            flash("localChangesError", Messages.get("lesson.version.local.cantMerge"));
        } else {
            try {
                lessonService.discardUserClone(IdentityUtils.getUserJid(), lesson.getJid());
            } catch (IOException e) {
                // do nothing
            }
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Commit version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
    }

    @Transactional(readOnly = true)
    public Result updateVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            return notFound();
        }

        lessonService.fetchUserClone(IdentityUtils.getUserJid(), lesson.getJid());

        if (!lessonService.updateUserClone(IdentityUtils.getUserJid(), lesson.getJid())) {
            flash("localChangesError", Messages.get("lesson.version.local.cantMerge"));
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
    }

    @Transactional(readOnly = true)
    public Result discardVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            return notFound();
        }

        try {
            lessonService.discardUserClone(IdentityUtils.getUserJid(), lesson.getJid());
            SandalphonControllerUtils.getInstance().addActivityLog("Discard version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
        } catch (IOException e) {
            return notFound();
        }
    }

    private Result showViewVersionLocalChanges(Form<VersionCommitForm> versionCommitForm, Lesson lesson, boolean isClean) {
        LazyHtml content = new LazyHtml(viewVersionLocalChangesView.render(versionCommitForm, lesson, isClean));
        appendSubtabsLayout(content, lesson);
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.version.local"), routes.LessonVersionController.viewVersionLocalChanges(lesson.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Versions - Local Changes");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks.add(new InternalLink(Messages.get("lesson.version.local"), routes.LessonVersionController.viewVersionLocalChanges(lesson.getId())));

        if (LessonControllerUtils.isAllowedToViewVersionHistory(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.version.history"), routes.LessonVersionController.listVersionHistory(lesson.getId())));
        }

        content.appendLayout(c -> subtabLayout.render(internalLinks.build(), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                .add(new InternalLink(Messages.get("lesson.version"), routes.LessonController.jumpToVersions(lesson.getId())))
                .add(lastLink)
                .build()
        );
    }

}
