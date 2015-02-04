package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.Submission;
import org.iatoki.judgels.commons.SubmissionException;
import org.iatoki.judgels.commons.SubmissionService;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.commons.SubmissionAdapters;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.GraderClientService;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.Problem;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpsertForm;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateViewClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateUpdateClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewSubmissionsView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

@Transactional
public final class ProgrammingProblemController extends Controller {

    private final ProblemService problemService;
    private final SubmissionService submissionService;
    private final ClientService clientService;
    private final GraderClientService graderClientService;

    public ProgrammingProblemController(ProblemService problemService, SubmissionService submissionService, ClientService clientService, GraderClientService graderClientService) {
        this.problemService = problemService;
        this.submissionService = submissionService;
        this.clientService = clientService;
        this.graderClientService = graderClientService;
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
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
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result create() {
        Form<UpsertForm> form = Form.form(UpsertForm.class);
        return showCreate(form);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.viewGeneral(id));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
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
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewStatement(long id) {
        String statement = problemService.getStatement(id);
        Problem problem = problemService.findProblemById(id);

        GradingConfig config = problemService.getGradingConfig(id);

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingType(problem.getGradingType()).renderViewStatement(routes.ProgrammingProblemController.postSubmit(id), problem.getName(), statement, config));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewStatement(id), routes.ProgrammingProblemController.updateStatement(id), c));
        appendTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewSubmissions(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Page<Submission> submissions = submissionService.pageSubmission(0, 20, "id", "asc", problem.getJid());
        LazyHtml content = new LazyHtml(viewSubmissionsView.render(submissions, problemId, "id", "asc", problem.getJid()));
        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(problemId))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewSubmission(long problemId, long submissionId) {
        Problem problem = problemService.findProblemById(problemId);
        GradingConfig config = problemService.getGradingConfig(problemId);
        Submission submission = submissionService.findSubmissionById(submissionId);


        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingType(problem.getGradingType()).renderViewSubmission(submissionId, submission.getVerdict(), submission.getScore(), submission.getDetails(), config));

        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(problemId))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result update(long id) {
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateGeneral(long id) {
        Problem problem = problemService.findProblemById(id);
        UpsertForm content = new UpsertForm();
        content.name = problem.getName();
        content.additionalNote = problem.getAdditionalNote();
        Form<UpsertForm> form = Form.form(UpsertForm.class).fill(content);

        return showUpdateGeneral(form, id);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateGeneral(long id) {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();
        problemService.updateProblem(id, form.get().name, form.get().additionalNote);
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateStatement(long id) {
        Problem problem = problemService.findProblemById(id);
        String statement = problemService.getStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));
        return showUpdateStatement(form, id, problem.getName());
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        problemService.updateStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<File> testDataFiles = problemService.getTestDataFiles(id);
        return showUpdateFiles(form, id, problem.getName(), testDataFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
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
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateGrading(long id) {
        Problem problem = problemService.findProblemById(id);
        GradingConfig config = problemService.getGradingConfig(id);
        List<File> testDataFiles = problemService.getTestDataFiles(id);
        List<File> helperFiles = problemService.getHelperFiles(id);

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createFormFromConfig(config);

        return showUpdateGrading(form, problem, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateGrading(long id) {
        Problem problem = problemService.findProblemById(id);

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createFormFromRequest(request());

        if (form.hasErrors()) {
            List<File> testDataFiles = problemService.getTestDataFiles(id);
            List<File> helperFiles = problemService.getHelperFiles(id);
            return showUpdateGrading(form, problem, testDataFiles, helperFiles);
        } else {
            GradingConfig config = GradingConfigAdapters.fromGradingType(problem.getGradingType()).createConfigFromForm(form);
            problemService.updateGradingConfig(id, config);
            return redirect(routes.ProgrammingProblemController.updateGrading(id));
        }
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        GradingConfig config = problemService.getGradingConfig(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();

        try {
            GradingSource source = SubmissionAdapters.fromGradingType(problem.getGradingType()).createGradingSource(config, body);
            submissionService.submit(problem.getJid(), problem.getGradingType(), problem.getTimeUpdate(), source);
        } catch (SubmissionException e) {
            flash("submissionError", e.getMessage());
            return redirect(routes.ProgrammingProblemController.viewStatement(problemId));
        }

        return redirect(routes.ProgrammingProblemController.viewSubmissions(problemId));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateClients(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
        List<Client> clients = clientService.findAllClients();

        return showUpdateClients(form, problem, clients, clientProblems);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
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
    @Authenticated(value = {LoggedIn.class, HasRole.class})
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
    @Authenticated(value = {LoggedIn.class, HasRole.class})
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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result downloadTestDataFile(long id, String filename) {
        File file = problemService.getTestDataFile(id, filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return ok("File does not exist :(");
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result delete(long id) {
        return TODO;
    }

    public Result verifyProblem() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String clientJid = form.get("clientId");
        String clientSecret = form.get("clientSecret");
        if (clientService.isClientExist(clientJid)) {
            Client client = clientService.findClientByJid(clientJid);
            System.out.println("A");
            if (client.getSecret().equals(clientSecret)) {
                System.out.println("B");
                String problemJid = form.get("problemJid");
                if (problemService.isProblemExistByProblemJid(problemJid)) {
                    return ok();
                } else {
                    return notFound();
                }
            } else {
                return forbidden();
            }
        } else {
            return notFound();
        }
    }

    public Result viewProblemStatementTOTP(String clientJid, String problemJid, int TOTP, String lang) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        if (clientService.isClientProblemInProblemByClientJid(problemJid, clientJid)) {
            ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);
            GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
            if (googleAuthenticator.authorize(new Base32().encodeAsString(clientProblem.getSecret().getBytes()), TOTP)) {
                System.out.println(problemService.getStatement(problemJid));
                return ok(problemService.getStatement(problemJid));
            } else {
                return forbidden();
            }
        } else {
            return notFound();
        }
    }

    public Result downloadGradingFiles() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderClientJid = form.get("graderClientJid");
        String graderClientSecret = form.get("graderClientSecret");
        String problemJid = form.get("problemJid");

        if (!problemService.isProblemExistByProblemJid(problemJid) || !graderClientService.verifyGraderClient(graderClientJid, graderClientSecret)) {
            return forbidden();
        }

        ByteArrayOutputStream os = problemService.getZippedGradingFilesStream(problemJid);
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition","attachment; filename=" + problemJid + ".zip");
        return ok(os.toByteArray()).as("application/zip");
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
        LazyHtml content = new LazyHtml(GradingConfigAdapters.fromGradingType(problem.getGradingType()).renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));
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
                new InternalLink(Messages.get("Submissions"), routes.ProgrammingProblemController.viewSubmissions(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.clients"), routes.ProgrammingProblemController.updateClients(id))
        ), c));

        content.appendLayout(c -> headingLayout.render("#" + id + ": " + problemName, c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();
        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index()));

        if (SandalphonUtils.hasRole("admin")) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("graderclient.clients"), routes.GraderClientController.index()));
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
