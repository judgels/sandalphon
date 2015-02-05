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
import org.iatoki.judgels.gabriel.GradingLanguage;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.GraderClientService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateHelperFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateMediaFilesForm;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.Problem;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateTestDataFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpsertForm;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateTestDataFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateHelperFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateMediaFilesView;
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
import play.filters.csrf.CSRF;
import play.filters.csrf.CSRFConf;
import play.filters.csrf.CSRFFilter;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

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
            Problem problem = problemService.createProblem(data.name, data.gradingEngine, data.additionalNote);

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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewStatement(long id) {
        String statement = problemService.getStatement(id);
        Problem problem = problemService.findProblemById(id);


        GradingConfig config = problemService.getGradingConfig(id);

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewStatement(routes.ProgrammingProblemController.postSubmit(id).absoluteURL(request()), problem.getName(), statement, config));
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

        GradingSource source = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).createGradingSourceFromPastSubmission(config, SandalphonProperties.getInstance().getSubmissionDir(), submission.getJid());

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewSubmission(submission, source));

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

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateFiles(long id) {
        return redirect(routes.ProgrammingProblemController.updateTestDataFiles(id));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateTestDataFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateTestDataFilesForm> form = Form.form(UpdateTestDataFilesForm.class);
        List<File> testDataFiles = problemService.getTestDataFiles(id);
        return showUpdateTestDataFiles(form, id, problem.getName(), testDataFiles);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateHelperFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateHelperFilesForm> form = Form.form(UpdateHelperFilesForm.class);
        List<File> helperFiles = problemService.getHelperFiles(id);
        return showUpdateHelperFiles(form, id, problem.getName(), helperFiles);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateMediaFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateMediaFilesForm> form = Form.form(UpdateMediaFilesForm.class);
        List<File> mediaFiles = problemService.getMediaFiles(id);
        return showUpdateMediaFiles(form, id, problem.getName(), mediaFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateFiles(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("testDataFile");
        if (file != null) {
            File testDataFile = file.getFile();
            problemService.uploadTestDataFile(id, testDataFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateTestDataFiles(id));
        }

        file = body.getFile("testDataFileZipped");
        if (file != null) {
            File testDataFile = file.getFile();
            problemService.uploadTestDataFileZipped(id, testDataFile);
            return redirect(routes.ProgrammingProblemController.updateTestDataFiles(id));
        }

        file = body.getFile("helperFile");
        if (file != null) {
            File helperFile = file.getFile();
            problemService.uploadHelperFile(id, helperFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateHelperFiles(id));
        }

        file = body.getFile("helperFileZipped");
        if (file != null) {
            File helperFileZipped = file.getFile();
            problemService.uploadHelperFileZipped(id, helperFileZipped);
            return redirect(routes.ProgrammingProblemController.updateHelperFiles(id));
        }

        file = body.getFile("mediaFile");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.uploadMediaFile(id, mediaFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateMediaFiles(id));
        }

        file = body.getFile("mediaFileZipped");
        if (file != null) {
            File mediaFileZipped = file.getFile();
            problemService.uploadMediaFileZipped(id, mediaFileZipped);
            return redirect(routes.ProgrammingProblemController.updateMediaFiles(id));
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

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createFormFromConfig(config);

        return showUpdateGrading(form, problem, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateGrading(long id) {
        Problem problem = problemService.findProblemById(id);

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createFormFromRequest(request());

        if (form.hasErrors()) {
            List<File> testDataFiles = problemService.getTestDataFiles(id);
            List<File> helperFiles = problemService.getHelperFiles(id);
            return showUpdateGrading(form, problem, testDataFiles, helperFiles);
        } else {
            GradingConfig config = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createConfigFromForm(form);
            problemService.updateGradingConfig(id, config);
            return redirect(routes.ProgrammingProblemController.updateGrading(id));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        GradingConfig config = problemService.getGradingConfig(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();

        String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

        try {
            GradingSource source = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).createGradingSourceFromNewSubmission(config, body);
            String submissionJid = submissionService.submit(problem.getJid(), problem.getGradingEngine(), gradingLanguage, problem.getTimeUpdate(), source);
            SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).storeSubmissionFiles(SandalphonProperties.getInstance().getSubmissionDir(), submissionJid, source);
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
            return notFound();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result downloadHelperFile(long id, String filename) {
        File file = problemService.getHelperFile(id, filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return notFound();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result downloadMediaFile(long id, String filename) {
        File file = problemService.getMediaFile(id, filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return notFound();
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

    public Result viewProblemStatementTOTP(String clientJid, String problemJid, int TOTP, String lang, String postSubmitUri) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        if (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid)) {
            return notFound();
        }

        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        if (!googleAuthenticator.authorize(new Base32().encodeAsString(clientProblem.getSecret().getBytes()), TOTP)) {
            return forbidden();
        }

        String statement = problemService.getStatement(problemJid);
        Problem problem = problemService.findProblemByJid(problemJid);

        GradingConfig config = problemService.getGradingConfig(problem.getId());

        Html html = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewStatement(postSubmitUri, problem.getName(), statement, config);
        return ok(html);
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
        LazyHtml content = new LazyHtml(GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.grading"), routes.ProgrammingProblemController.updateGrading(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateTestDataFiles(Form<UpdateTestDataFilesForm> form, long id, String problemName, List<File> testDataFiles) {
        LazyHtml content = new LazyHtml(updateTestDataFilesView.render(form, id, testDataFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateHelperFiles(Form<UpdateHelperFilesForm> form, long id, String problemName, List<File> helperFiles) {
        LazyHtml content = new LazyHtml(updateHelperFilesView.render(form, id, helperFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateMediaFiles(Form<UpdateMediaFilesForm> form, long id, String problemName, List<File> mediaFiles) {
        LazyHtml content = new LazyHtml(updateMediaFilesView.render(form, id, mediaFiles));
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
