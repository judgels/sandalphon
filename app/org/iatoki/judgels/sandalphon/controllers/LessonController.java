package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.play.views.html.layouts.headingLayout;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.LessonStatementUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.LessonCreateForm;
import org.iatoki.judgels.sandalphon.forms.LessonUpdateForm;
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
        Page<Lesson> pageOfLessons = lessonService.getPageOfLessons(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString, IdentityUtils.getUserJid(), ControllerUtils.getInstance().isAdmin());

        LazyHtml content = new LazyHtml(listLessonsView.render(pageOfLessons, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("lesson.list"), new InternalLink(Messages.get("commons.create"), routes.LessonController.createLesson()), c));

        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lessons");

        ControllerUtils.getInstance().addActivityLog("Open allowed lessons <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result createLesson() {
        Form<LessonCreateForm> lessonCreateForm = Form.form(LessonCreateForm.class);

        ControllerUtils.getInstance().addActivityLog("Try to create lesson <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreateLesson(lessonCreateForm);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreateLesson() {
        Form<LessonCreateForm> lessonCreateForm = Form.form(LessonCreateForm.class).bindFromRequest();

        if (formHasErrors(lessonCreateForm)) {
            return showCreateLesson(lessonCreateForm);
        }

        LessonCreateForm lessonCreateData = lessonCreateForm.get();

        Lesson lesson;
        try {
            lesson = lessonService.createLesson(lessonCreateData.name, lessonCreateData.additionalNote, lessonCreateData.initLanguageCode);
            lessonService.updateStatement(null, lesson.getId(), lessonCreateData.initLanguageCode, LessonStatementUtils.getDefaultStatement(lessonCreateData.initLanguageCode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        lessonService.initRepository(IdentityUtils.getUserJid(), lesson.getJid());

        LessonControllerUtils.setCurrentStatementLanguage(lessonCreateData.initLanguageCode);
        return redirect(routes.LessonController.index());
    }

    public Result enterLesson(long lessonId) {
        ControllerUtils.getInstance().addActivityLog("Enter lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonController.jumpToStatement(lessonId));
    }

    public Result jumpToStatement(long lessonId) {
        ControllerUtils.getInstance().addActivityLog("Jump to lesson statement " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonStatementController.viewStatement(lessonId));
    }

    public Result jumpToVersions(long lessonId) {
        ControllerUtils.getInstance().addActivityLog("Jump to lesson version " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonVersionController.viewVersionLocalChanges(lessonId));
    }

    public Result jumpToPartners(long lessonId) {
        ControllerUtils.getInstance().addActivityLog("Jump to lesson partner " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonPartnerController.viewPartners(lessonId));
    }

    public Result jumpToClients(long lessonId) {
        ControllerUtils.getInstance().addActivityLog("Jump to lesson client " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonClientController.updateClientLessons(lessonId));
    }

    @Transactional(readOnly = true)
    public Result viewLesson(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        LazyHtml content = new LazyHtml(viewLessonView.render(lesson));
        appendSubtabs(content, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getName(), new InternalLink(Messages.get("lesson.enter"), routes.LessonController.enterLesson(lesson.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                    .add(new InternalLink(Messages.get("lesson.view"), routes.LessonController.viewLesson(lesson.getId())))
                    .build()
        );
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - View");

        ControllerUtils.getInstance().addActivityLog("View lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateLesson(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAllowedToUpdateLesson(lessonService, lesson)) {
            return redirect(routes.LessonController.viewLesson(lesson.getId()));
        }

        LessonUpdateForm lessonUpdateData = new LessonUpdateForm();
        lessonUpdateData.name = lesson.getName();
        lessonUpdateData.additionalNote = lesson.getAdditionalNote();

        Form<LessonUpdateForm> lessonUpdateForm = Form.form(LessonUpdateForm.class).fill(lessonUpdateData);

        ControllerUtils.getInstance().addActivityLog("Try to update lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

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
        if (formHasErrors(lessonUpdateForm)) {
            return showUpdateLesson(lessonUpdateForm, lesson);
        }

        LessonUpdateForm lessonUpdateData = lessonUpdateForm.get();
        lessonService.updateLesson(lessonId, lessonUpdateData.name, lessonUpdateData.additionalNote);

        ControllerUtils.getInstance().addActivityLog("Update lesson " + lesson.getName() + ".");

        return redirect(routes.LessonController.viewLesson(lesson.getId()));
    }

    public Result switchLanguage(long lessonId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        LessonControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch language to " + languageCode + " of lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(request().getHeader("Referer"));
    }

    private Result showCreateLesson(Form<LessonCreateForm> lessonCreateForm) {
        LazyHtml content = new LazyHtml(createLessonView.render(lessonCreateForm));
        content.appendLayout(c -> headingLayout.render(Messages.get("lesson.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index()),
                new InternalLink(Messages.get("lesson.create"), routes.LessonController.createLesson())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateLesson(Form<LessonUpdateForm> lessonUpdateForm, Lesson lesson) {
        LazyHtml content = new LazyHtml(updateLessonView.render(lessonUpdateForm, lesson));
        appendSubtabs(content, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getName(), new InternalLink(Messages.get("lesson.enter"), routes.LessonController.enterLesson(lesson.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                .add(new InternalLink(Messages.get("lesson.update"), routes.LessonController.updateLesson(lesson.getId())))
                .build()
        );
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update");

        return ControllerUtils.getInstance().lazyOk(content);
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
