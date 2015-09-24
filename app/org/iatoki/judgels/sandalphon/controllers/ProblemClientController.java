package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.jophiel.BasicActivityKeys;
import org.iatoki.judgels.play.IdentityUtils;
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
import org.iatoki.judgels.sandalphon.views.html.problem.client.editClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.client.viewClientProblemView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProblemClientController extends AbstractJudgelsController {

    private static final String PROBLEM = "problem";
    private static final String CLIENT = "client";

    private final ClientService clientService;
    private final ProblemService problemService;

    @Inject
    public ProblemClientController(ClientService clientService, ProblemService problemService) {
        this.clientService = clientService;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            return notFound();
        }

        Form<ClientProblemUpsertForm> clientProblemUpsertForm = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
        List<Client> clients = clientService.getClients();

        return showEditClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            return notFound();
        }

        Form<ClientProblemUpsertForm> clientProblemUpsertForm = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if (formHasErrors(clientProblemUpsertForm)) {
            List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
            List<Client> clients = clientService.getClients();

            return showEditClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
        }

        ClientProblemUpsertForm clientProblemUpsertData = clientProblemUpsertForm.get();
        if (!clientService.clientExistsByJid(clientProblemUpsertData.clientJid) || clientService.isClientAuthorizedForProblem(problem.getJid(), clientProblemUpsertData.clientJid)) {
            List<ClientProblem> clientProblems = clientService.getClientProblemsByProblemJid(problem.getJid());
            List<Client> clients = clientService.getClients();

            return showEditClientProblems(clientProblemUpsertForm, problem, clients, clientProblems);
        }

        clientService.createClientProblem(problem.getJid(), clientProblemUpsertData.clientJid, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        SandalphonControllerUtils.getInstance().addActivityLog(BasicActivityKeys.ADD_IN.construct(PROBLEM, problem.getJid(), problem.getSlug(), CLIENT, clientProblemUpsertData.clientJid, clientService.findClientByJid(clientProblemUpsertData.clientJid).getName()));

        return redirect(routes.ProblemClientController.editClientProblems(problem.getId()));
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

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showEditClientProblems(Form<ClientProblemUpsertForm> clientProblemUpsertForm, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(editClientProblemsView.render(clientProblemUpsertForm, problem.getId(), clients, clientProblems));
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.client.list"), routes.ProblemClientController.editClientProblems(problem.getId())));
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
