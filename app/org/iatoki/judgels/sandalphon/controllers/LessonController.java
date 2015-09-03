package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.headingLayout;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonStatement;
import org.iatoki.judgels.sandalphon.LessonStatementUtils;
import org.iatoki.judgels.sandalphon.ProblemStatementUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.LessonCreateForm;
import org.iatoki.judgels.sandalphon.forms.LessonUpdateForm;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.views.html.lesson.createLessonView;
import org.iatoki.judgels.sandalphon.views.html.lesson.listLessonsView;
import org.iatoki.judgels.sandalphon.views.html.lesson.updateLessonView;
import org.iatoki.judgels.sandalphon.views.html.lesson.viewLessonView;
import play.data.DynamicForm;
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

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class LessonController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final LessonService lessonService;

    @Inject
    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result index() {
        return listLessons(0, "timeUpdate", "desc", "");
    }

    @Transactional(readOnly = true)
    public Result listLessons(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Lesson> pageOfLessons = lessonService.getPageOfLessons(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString, IdentityUtils.getUserJid(), SandalphonControllerUtils.getInstance().isAdmin());

        LazyHtml content = new LazyHtml(listLessonsView.render(pageOfLessons, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("lesson.list"), new InternalLink(Messages.get("commons.create"), routes.LessonController.createLesson()), c));

        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index())
        ));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lessons");

        SandalphonControllerUtils.getInstance().addActivityLog("Open allowed lessons <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result createLesson() {
        Form<LessonCreateForm> lessonCreateForm = Form.form(LessonCreateForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to create lesson <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreateLesson(lessonCreateForm);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreateLesson() {
        Form<LessonCreateForm> lessonCreateForm = Form.form(LessonCreateForm.class).bindFromRequest();

        if (lessonService.lessonExistsBySlug(lessonCreateForm.get().slug)) {
            lessonCreateForm.reject("slug", Messages.get("error.lesson.slugExists"));
        }

        if (formHasErrors(lessonCreateForm)) {
            return showCreateLesson(lessonCreateForm);
        }

        LessonCreateForm lessonCreateData = lessonCreateForm.get();

        Lesson lesson;
        try {
            lesson = lessonService.createLesson(lessonCreateData.slug, lessonCreateData.additionalNote, lessonCreateData.initLanguageCode);
            lessonService.updateStatement(null, lesson.getId(), lessonCreateData.initLanguageCode, new LessonStatement(ProblemStatementUtils.getDefaultTitle(lessonCreateData.initLanguageCode), LessonStatementUtils.getDefaultText(lessonCreateData.initLanguageCode)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        lessonService.initRepository(IdentityUtils.getUserJid(), lesson.getJid());

        LessonControllerUtils.setCurrentStatementLanguage(lessonCreateData.initLanguageCode);
        return redirect(routes.LessonController.index());
    }

    public Result enterLesson(long lessonId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Enter lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonController.jumpToStatement(lessonId));
    }

    public Result jumpToStatement(long lessonId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to lesson statement " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonStatementController.viewStatement(lessonId));
    }

    public Result jumpToVersions(long lessonId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to lesson version " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonVersionController.viewVersionLocalChanges(lessonId));
    }

    public Result jumpToPartners(long lessonId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to lesson partner " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonPartnerController.viewPartners(lessonId));
    }

    public Result jumpToClients(long lessonId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to lesson client " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonClientController.updateClientLessons(lessonId));
    }

    @Transactional(readOnly = true)
    public Result viewLesson(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        LazyHtml content = new LazyHtml(viewLessonView.render(lesson));
        appendSubtabs(content, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getSlug(), new InternalLink(Messages.get("lesson.enter"), routes.LessonController.enterLesson(lesson.getId())), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                    .add(new InternalLink(Messages.get("lesson.view"), routes.LessonController.viewLesson(lesson.getId())))
                    .build()
        );
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - View");

        SandalphonControllerUtils.getInstance().addActivityLog("View lesson " + lesson.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateLesson(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAllowedToUpdateLesson(lessonService, lesson)) {
            return redirect(routes.LessonController.viewLesson(lesson.getId()));
        }

        LessonUpdateForm lessonUpdateData = new LessonUpdateForm();
        lessonUpdateData.slug = lesson.getSlug();
        lessonUpdateData.additionalNote = lesson.getAdditionalNote();

        Form<LessonUpdateForm> lessonUpdateForm = Form.form(LessonUpdateForm.class).fill(lessonUpdateData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update lesson " + lesson.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdateLesson(lessonUpdateForm, lesson);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateLesson(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAllowedToUpdateLesson(lessonService, lesson)) {
            return notFound();
        }

        Form<LessonUpdateForm> lessonUpdateForm = Form.form(LessonUpdateForm.class).bindFromRequest();

        if (!lesson.getSlug().equals(lessonUpdateForm.get().slug) && lessonService.lessonExistsBySlug(lessonUpdateForm.get().slug)) {
            lessonUpdateForm.reject("slug", Messages.get("error.lesson.slugExists"));
        }

        if (formHasErrors(lessonUpdateForm)) {
            return showUpdateLesson(lessonUpdateForm, lesson);
        }

        LessonUpdateForm lessonUpdateData = lessonUpdateForm.get();
        lessonService.updateLesson(lessonId, lessonUpdateData.slug, lessonUpdateData.additionalNote);

        SandalphonControllerUtils.getInstance().addActivityLog("Update lesson " + lesson.getSlug() + ".");

        return redirect(routes.LessonController.viewLesson(lesson.getId()));
    }

    public Result switchLanguage(long lessonId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        LessonControllerUtils.setCurrentStatementLanguage(languageCode);

        SandalphonControllerUtils.getInstance().addActivityLog("Switch language to " + languageCode + " of lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(request().getHeader("Referer"));
    }

    private Result showCreateLesson(Form<LessonCreateForm> lessonCreateForm) {
        LazyHtml content = new LazyHtml(createLessonView.render(lessonCreateForm));
        content.appendLayout(c -> headingLayout.render(Messages.get("lesson.create"), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index()),
                new InternalLink(Messages.get("lesson.create"), routes.LessonController.createLesson())
        ));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Create");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateLesson(Form<LessonUpdateForm> lessonUpdateForm, Lesson lesson) {
        LazyHtml content = new LazyHtml(updateLessonView.render(lessonUpdateForm, lesson));
        appendSubtabs(content, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getSlug(), new InternalLink(Messages.get("lesson.enter"), routes.LessonController.enterLesson(lesson.getId())), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                .add(new InternalLink(Messages.get("lesson.update"), routes.LessonController.updateLesson(lesson.getId())))
                .build()
        );
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabs(LazyHtml content, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.LessonController.viewLesson(lesson.getId())));

        if (LessonControllerUtils.isAllowedToUpdateLesson(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.LessonController.updateLesson(lesson.getId())));
        }

        content.appendLayout(c -> subtabLayout.render(internalLinks.build(), c));
    }
}
