package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.gabriel.blackbox.BlackBoxGradingConfig;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.Problem;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.programming.SubmitForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpsertForm;
import org.iatoki.judgels.sandalphon.programming.Submission;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateViewClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateUpdateClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewSubmissionsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.util.List;
import java.util.Map;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemController extends Controller {

    private final ProblemService problemService;
    private final ClientService clientService;

    public ProgrammingProblemController(ProblemService problemService, ClientService clientService) {
        this.problemService = problemService;
        this.clientService = clientService;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Transactional
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<Problem> currentPage = problemService.pageProblem(page, 20, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.programming.list"), new InternalLink(Messages.get("problem.programming.create"), routes.ProgrammingProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<UpsertForm> form = Form.form(UpsertForm.class);
        return showCreate(form);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postCreate() {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();

        if (form.hasErrors()) {
            return showCreate(form);
        } else {
            UpsertForm data = form.get();
            Problem problem = problemService.createProblem(data.name, data.gradingType, data.additionalNote);

            return redirect(routes.ProgrammingProblemController.update(problem.getId()));
        }
    }

    private Result showCreate(Form<UpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.programming.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.create"), routes.ProgrammingProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.viewGeneral(id));
    }

    @Transactional
    public Result viewGeneral(long id) {
        Problem problem = problemService.findProblemById(id);
        LazyHtml content = new LazyHtml(viewGeneralView.render(problem));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewGeneral(id), routes.ProgrammingProblemController.updateGeneral(id), c));
        appendTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @AddCSRFToken
    @Transactional
    public Result viewStatement(long id) {
        String statement = problemService.getStatement(id);
        Problem problem = problemService.findProblemById(id);

        Form<SubmitForm> form = Form.form(SubmitForm.class);
        LazyHtml content = new LazyHtml(viewStatementView.render(form, statement, id));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewStatement(id), routes.ProgrammingProblemController.updateStatement(id), c));
        appendTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Transactional
    public Result viewSubmissions(long id) {
        Problem problem = problemService.findProblemById(id);
        Page<Submission> submissions = problemService.pageSubmission(0, 20, "id", "asc", problem.getJid());
        LazyHtml content = new LazyHtml(viewSubmissionsView.render(submissions, "id", "asc", problem.getJid()));
        appendTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    public Result update(long id) {
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGeneral(long id) {
        Problem problem = problemService.findProblemById(id);
        UpsertForm content = new UpsertForm();
        content.name = problem.getName();
        content.additionalNote = problem.getAdditionalNote();
        Form<UpsertForm> form = Form.form(UpsertForm.class).fill(content);

        return showUpdateGeneral(form, id);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGeneral(long id) {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();
        problemService.updateProblem(id, form.get().name, form.get().additionalNote);
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateStatement(long id) {
        Problem problem = problemService.findProblemById(id);
        String statement = problemService.getStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));
        return showUpdateStatement(form, id, problem.getName());
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        problemService.updateStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<File> testDataFiles = problemService.getTestDataFiles(id);
        return showUpdateFiles(form, id, problem.getName(), testDataFiles);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateFiles(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            File evaluatorFile = file.getFile();

            problemService.uploadTestDataFile(id, evaluatorFile, file.getFilename());
        }

        return redirect(routes.ProgrammingProblemController.updateFiles(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGrading(long id) {
        Problem problem = problemService.findProblemById(id);
        String gradingConfigJson = problemService.getGradingConfig(id);
        List<File> testDataFiles = problemService.getTestDataFiles(id);
        List<File> helperFiles = problemService.getHelperFiles(id);

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createFormFromConfigJson(gradingConfigJson);

        return showUpdateGrading(form, problem, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGrading(long id) {
        Problem problem = problemService.findProblemById(id);

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createFormFromRequest(request());

        if (form.hasErrors()) {
            List<File> testDataFiles = problemService.getTestDataFiles(id);
            List<File> helperFiles = problemService.getHelperFiles(id);
            return showUpdateGrading(form, problem, testDataFiles, helperFiles);
        } else {
            BlackBoxGradingConfig config = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createConfigFromForm(form);
            String configAsJson = new Gson().toJson(config);
            problemService.updateGradingConfig(id, configAsJson);
            return redirect(routes.ProgrammingProblemController.updateGrading(id));
        }
    }

    @RequireCSRFCheck
    @Transactional
    public Result postSubmit(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            try {
                File sourceFile = file.getFile();

                byte[] sourceFileData = FileUtils.readFileToByteArray(sourceFile);
                Map<String, byte[]> sourceFiles = ImmutableMap.of(file.getFilename(), sourceFileData);

                problemService.submit(id, sourceFiles);
                return ok("dah disubmit");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return redirect(routes.ProgrammingProblemController.viewStatement(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateClients(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
        List<Client> clients = clientService.findAllClients();

        return showUpdateClients(form, problem, clients, clientProblems);
    }

    @RequireCSRFCheck
    public Result postUpdateClients(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if ((form.hasErrors() || form.hasGlobalErrors())) {
            List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
            List<Client> clients = clientService.findAllClients();
            return showUpdateClients(form, problem, clients, clientProblems);
        } else {
            ClientProblemUpsertForm clientProblemUpsertForm = form.get();
            if ((clientService.isClientExist(clientProblemUpsertForm.clientJid)) && (!clientService.isClientProblemInProblemByClientJid(problem.getJid(), clientProblemUpsertForm.clientJid))) {
                clientService.createClientProblem(problem.getJid(), clientProblemUpsertForm.clientJid);
                return redirect(routes.ProgrammingProblemController.updateClients(problem.getId()));
            } else {
                List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
                List<Client> clients = clientService.findAllClients();
                return showUpdateClients(form, problem, clients, clientProblems);
            }
        }
    }

    public Result updateViewClients(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            LazyHtml content = new LazyHtml(updateViewClientProblemView.render(problem, clientProblem));
            appendTabsLayout(content, problemId, problem.getName());
            content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                    new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                    new InternalLink(Messages.get("problem.programming.update.general"), routes.ProgrammingProblemController.updateGeneral(problemId))
            ), c));
            appendTemplateLayout(content);

            return lazyOk(content);
        } else {
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
        }
    }


    @AddCSRFToken
    @Transactional
    public Result updateUpdateClients(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
            form.fill(new ClientProblemUpsertForm(clientProblem));
            List<Client> clients = clientService.findAllClients();

            return showUpdateUpdateClients(form, problem, clientProblem, clients);
        } else {
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
        }
    }

    @RequireCSRFCheck
    public Result postUpdateUpdateClients(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if ((form.hasErrors() || form.hasGlobalErrors())) {
            List<Client> clients = clientService.findAllClients();
            return showUpdateUpdateClients(form, problem, clientProblem, clients);
        } else {
            ClientProblemUpsertForm clientProblemUpsertForm = form.get();
            if ((clientProblem.getProblemJid().equals(problem.getJid())) && (clientService.isClientExist(clientProblemUpsertForm.clientJid)) && (!clientService.isClientProblemInProblemByClientJid(problem.getJid(), clientProblemUpsertForm.clientJid))) {
                clientService.updateClientProblem(clientProblem.getId(), clientProblemUpsertForm.clientJid);
                return redirect(routes.ProgrammingProblemController.updateClients(problem.getId()));
            } else {
                List<Client> clients = clientService.findAllClients();
                return showUpdateUpdateClients(form, problem, clientProblem, clients);
            }
        }
    }

    public Result deleteClients(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            clientService.deleteClientProblem(clientProblem.getId());

            return redirect(routes.ProgrammingProblemController.updateClients(problem.getId()));
        } else {
            return forbidden();
        }
    }

    @Transactional
    public Result downloadTestDataFile(long id, String filename) {
        File file = problemService.getTestDataFile(id, filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return ok("File does not exist :(");
        }
    }

    public Result delete(long id) {
        return TODO;
    }

    private Result showUpdateGeneral(Form<UpsertForm> form, long id) {
        LazyHtml content = new LazyHtml(updateGeneralView.render(form, id));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewGeneral(id), routes.ProgrammingProblemController.updateGeneral(id), c));
        appendTabsLayout(content, id, form.get().name);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.general"), routes.ProgrammingProblemController.updateGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long id, String problemName) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, id));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewStatement(id), routes.ProgrammingProblemController.updateStatement(id), c));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.statement"), routes.ProgrammingProblemController.updateStatement(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateGrading(Form<?> form, Problem problem, List<File> testDataFiles, List<File> helperFiles) {
        LazyHtml content = new LazyHtml(GradingConfigAdapters.fromGradingType(problem.getGradingType()).renderForm(form, problem, testDataFiles, helperFiles));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.grading"), routes.ProgrammingProblemController.updateGrading(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateFiles(Form<UpdateFilesForm> form, long id, String problemName, List<File> testDataFiles) {
        LazyHtml content = new LazyHtml(updateFilesView.render(form, id, testDataFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateClients(Form<ClientProblemUpsertForm> form, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemView.render(form, problem.getId(), clients, clientProblems));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.clients"), routes.ProgrammingProblemController.updateClients(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateUpdateClients(Form<ClientProblemUpsertForm> form, Problem problem, ClientProblem clientProblem, List<Client> clients) {
        LazyHtml content = new LazyHtml(updateUpdateClientProblemView.render(form, problem.getId(), clientProblem.getId(), clients));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.clients"), routes.ProgrammingProblemController.updateClients(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private void appendTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.update.tab.general"), routes.ProgrammingProblemController.updateGeneral(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.statement"), routes.ProgrammingProblemController.updateStatement(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.grading"), routes.ProgrammingProblemController.updateGrading(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.files"), routes.ProgrammingProblemController.updateFiles(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.clients"), routes.ProgrammingProblemController.updateClients(id))
        ), c));

        content.appendLayout(c -> headingLayout.render("#" + id + ": " + problemName, c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();
        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index()));

        if (SandalphonUtils.hasRole("admin")) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("userRole.userRoles"), routes.UserRoleController.index()));
        }

        content.appendLayout(c -> leftSidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        "#",
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(request())).absoluteURL(request()),
                        internalLinkBuilder.build(), c)
        );
        content.appendLayout(c -> headerFooterLayout.render(c));
        content.appendLayout(c -> baseLayout.render("TODO", c));
    }

    private Result downloadFile(File file) {
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition","attachment; filename=" + file.getName());
        return ok(file);
    }

    private Result lazyOk(LazyHtml content) {
        return getResult(content, Http.Status.OK);
    }

    private Result getResult(LazyHtml content, int statusCode) {
        switch (statusCode) {
            case Http.Status.OK:
                return ok(content.render(0));
            case Http.Status.NOT_FOUND:
                return notFound(content.render(0));
            default:
                return badRequest(content.render(0));
        }
    }
}
