package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ClientLessonUpsertForm;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.views.html.lesson.client.updateClientLessonsView;
import org.iatoki.judgels.sandalphon.views.html.lesson.client.viewClientLessonView;
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
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class LessonClientController extends AbstractJudgelsController {

    private final LessonService lessonService;
    private final ClientService clientService;

    @Inject
    public LessonClientController(LessonService lessonService, ClientService clientService) {
        this.lessonService = lessonService;
        this.clientService = clientService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateClientLessons(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageClients(lessonService, lesson)) {
            Form<ClientLessonUpsertForm> form = Form.form(ClientLessonUpsertForm.class);
            List<ClientLesson> clientLessons = clientService.findAllClientLessonByLessonJid(lesson.getJid());
            List<Client> clients = clientService.findAllClients();

            ControllerUtils.getInstance().addActivityLog("Try to update client on lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateClientLessons(form, lesson, clients, clientLessons);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateClientLessons(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageClients(lessonService, lesson)) {
            Form<ClientLessonUpsertForm> form = Form.form(ClientLessonUpsertForm.class).bindFromRequest();

            if (form.hasErrors() || form.hasGlobalErrors()) {
                List<ClientLesson> clientLessons = clientService.findAllClientLessonByLessonJid(lesson.getJid());
                List<Client> clients = clientService.findAllClients();
                return showUpdateClientLessons(form, lesson, clients, clientLessons);
            } else {
                ClientLessonUpsertForm clientLessonUpsertForm = form.get();
                if ((clientService.clientExistsByClientJid(clientLessonUpsertForm.clientJid)) && (!clientService.isClientLessonInLessonByClientJid(lesson.getJid(), clientLessonUpsertForm.clientJid))) {
                    clientService.createClientLesson(lesson.getJid(), clientLessonUpsertForm.clientJid);

                    ControllerUtils.getInstance().addActivityLog("Add client " + clientLessonUpsertForm.clientJid + " to lesson " + lesson.getName() + ".");

                    return redirect(routes.LessonClientController.updateClientLessons(lesson.getId()));
                } else {
                    List<ClientLesson> clientLessons = clientService.findAllClientLessonByLessonJid(lesson.getJid());
                    List<Client> clients = clientService.findAllClients();
                    return showUpdateClientLessons(form, lesson, clients, clientLessons);
                }
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result viewClientLesson(long lessonId, long clientLessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        ClientLesson clientLesson = clientService.findClientLessonByClientLessonId(clientLessonId);
        if (clientLesson.getLessonJid().equals(lesson.getJid())) {
            LazyHtml content = new LazyHtml(viewClientLessonView.render(lesson, clientLesson));
            LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
            LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
            LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.client.client"), routes.LessonClientController.viewClientLesson(lessonId, clientLessonId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Statement");

            ControllerUtils.getInstance().addActivityLog("View client " + clientLesson.getClientName() + " to lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    private Result showUpdateClientLessons(Form<ClientLessonUpsertForm> form, Lesson lesson, List<Client> clients, List<ClientLesson> clientLessons) {
        LazyHtml content = new LazyHtml(updateClientLessonsView.render(form, lesson.getId(), clients, clientLessons));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.client.list"), routes.LessonClientController.updateClientLessons(lesson.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Client");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                .add(new InternalLink(Messages.get("lesson.client"), routes.LessonController.jumpToClients(lesson.getId())))
                .add(lastLink)
                .build()
        );
    }
}
