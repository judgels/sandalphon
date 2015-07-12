package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.GitCommit;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.BaseController;
import org.iatoki.judgels.play.views.html.layouts.accessTypesLayout;
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
public final class LessonVersionController extends BaseController {

    private final LessonService lessonService;

    @Inject
    public LessonVersionController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result listVersionHistory(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToViewVersionHistory(lessonService, lesson)) {
            List<GitCommit> versions = lessonService.getVersions(IdentityUtils.getUserJid(), lesson.getJid());
            boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());
            boolean isAllowedToRestoreVersionHistory = isClean && LessonControllerUtils.isAllowedToRestoreVersionHistory(lessonService, lesson);

            LazyHtml content = new LazyHtml(listVersionsView.render(versions, lesson.getId(), isAllowedToRestoreVersionHistory));
            appendSubtabsLayout(content, lesson);
            LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
            LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
            LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.version.history"), routes.LessonVersionController.listVersionHistory(lesson.getId())));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Versions - History");

            ControllerUtils.getInstance().addActivityLog("List version history of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result restoreVersionHistory(long lessonId, String hash) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());

        if (isClean && LessonControllerUtils.isAllowedToRestoreVersionHistory(lessonService, lesson)) {
            lessonService.restore(lesson.getJid(), hash);

            ControllerUtils.getInstance().addActivityLog("Restore version history " + hash + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.LessonVersionController.listVersionHistory(lesson.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result viewVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());

            Form<VersionCommitForm> form = Form.form(VersionCommitForm.class);

            ControllerUtils.getInstance().addActivityLog("View version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showViewVersionLocalChanges(form, lesson, isClean);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCommitVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            Form<VersionCommitForm> form = Form.form(VersionCommitForm.class).bindFromRequest();
            if (form.hasErrors() || form.hasGlobalErrors()) {
                boolean isClean = !lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid());
                return showViewVersionLocalChanges(form, lesson, isClean);
            }

            VersionCommitForm data = form.get();

            if (lessonService.fetchUserClone(IdentityUtils.getUserJid(), lesson.getJid())) {
                flash("localChangesError", Messages.get("lesson.version.local.cantCommit"));
            } else if (!lessonService.commitThenMergeUserClone(IdentityUtils.getUserJid(), lesson.getJid(), data.title, data.description)) {
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

            ControllerUtils.getInstance().addActivityLog("Commit version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result updateVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            lessonService.fetchUserClone(IdentityUtils.getUserJid(), lesson.getJid());

            if (!lessonService.updateUserClone(IdentityUtils.getUserJid(), lesson.getJid())) {
                flash("localChangesError", Messages.get("lesson.version.local.cantMerge"));
            }

            ControllerUtils.getInstance().addActivityLog("Update version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result discardVersionLocalChanges(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isPartnerOrAbove(lessonService, lesson)) {
            try {
                lessonService.discardUserClone(IdentityUtils.getUserJid(), lesson.getJid());
                ControllerUtils.getInstance().addActivityLog("Discard version changes of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.LessonVersionController.viewVersionLocalChanges(lesson.getId()));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    private Result showViewVersionLocalChanges(Form<VersionCommitForm> form, Lesson lesson, boolean isClean) {
        LazyHtml content = new LazyHtml(viewVersionLocalChangesView.render(form, lesson, isClean));
        appendSubtabsLayout(content, lesson);
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.version.local"), routes.LessonVersionController.viewVersionLocalChanges(lesson.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Versions - Local Changes");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks.add(new InternalLink(Messages.get("lesson.version.local"), routes.LessonVersionController.viewVersionLocalChanges(lesson.getId())));

        if (LessonControllerUtils.isAllowedToViewVersionHistory(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.version.history"), routes.LessonVersionController.listVersionHistory(lesson.getId())));
        }

        content.appendLayout(c -> accessTypesLayout.render(internalLinks.build(), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                .add(new InternalLink(Messages.get("lesson.version"), routes.LessonController.jumpToVersions(lesson.getId())))
                .add(lastLink)
                .build()
        );
    }

}
