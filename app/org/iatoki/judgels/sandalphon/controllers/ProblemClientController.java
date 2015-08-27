package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.client.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.client.viewClientProblemView;
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
public final class ProblemClientController extends AbstractJudgelsController {

    private final ClientService clientService;
    private final ProblemService problemService;

    @Inject
    public ProblemClientController(ClientService clientService, ProblemService problemService) {
        this.clientService = clientService;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            return notFound();
        }

        Form<ClientProblemUpsertForm> clientProblemUpsertForm = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
        List<Client> clients = clientService.getClients();

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update client on problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdateClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            return notFound();
        }

        Form<ClientProblemUpsertForm> clientProblemUpsertForm = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if (formHasErrors(clientProblemUpsertForm)) {
            List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
            List<Client> clients = clientService.getClients();

            return showUpdateClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
        }

        ClientProblemUpsertForm clientProblemUpsertData = clientProblemUpsertForm.get();
        if (!clientService.clientExistsByJid(clientProblemUpsertData.clientJid) || clientService.isClientAuthorizedForProblem(problem.getJid(), clientProblemUpsertData.clientJid)) {
            List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
            List<Client> clients = clientService.getClients();

            return showUpdateClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
        }

        clientService.createClientProblem(problem.getJid(), clientProblemUpsertData.clientJid);

        SandalphonControllerUtils.getInstance().addActivityLog("Add client " + clientProblemUpsertData.clientJid + " to problem " + problem.getName() + ".");

        return redirect(routes.ProblemClientController.updateClientProblems(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result viewClientProblem(long problemId, long clientProblemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemById(clientProblemId);
        if (!clientProblem.getProblemJid().equals(problem.getJid())) {
            return notFound();
        }

        LazyHtml content = new LazyHtml(viewClientProblemView.render(problem, clientProblem));
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.client.client"), routes.ProblemClientController.viewClientProblem(problemId, clientProblemId)));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        SandalphonControllerUtils.getInstance().addActivityLog("View client " + clientProblem.getClientName() + " to problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateClientProblems(Form<ClientProblemUpsertForm> clientProblemUpsertForm, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemsView.render(clientProblemUpsertForm, problem.getId(), clients, clientProblems));
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.client.list"), routes.ProblemClientController.updateClientProblems(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Client");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
