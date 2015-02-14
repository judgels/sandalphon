package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.commons.Submission;
import org.iatoki.judgels.sandalphon.commons.SubmissionException;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.sandalphon.commons.SubmissionAdapters;
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
import org.iatoki.judgels.sandalphon.GraderService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.programming.forms.UpdateHelperFilesForm;
import org.iatoki.judgels.sandalphon.programming.forms.UpdateMediaFilesForm;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.Problem;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.programming.forms.UpdateTestDataFilesForm;
import org.iatoki.judgels.sandalphon.programming.forms.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.programming.forms.UpsertForm;
import org.iatoki.judgels.sandalphon.programming.ProblemSubmission;
import org.iatoki.judgels.sandalphon.programming.ProblemSubmissionService;
import org.iatoki.judgels.sandalphon.programming.views.html.createView;
import org.iatoki.judgels.sandalphon.programming.views.html.listView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateTestDataFilesView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateHelperFilesView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateMediaFilesView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateGeneralView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateStatementView;
import org.iatoki.judgels.sandalphon.programming.views.html.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.programming.views.html.viewClientProblemView;
import org.iatoki.judgels.sandalphon.programming.views.html.viewGeneralView;
import org.iatoki.judgels.sandalphon.programming.views.html.viewSubmissionsView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
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

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final ProblemSubmissionService submissionService;
    private final ClientService clientService;
    private final GraderService graderService;

    public ProgrammingProblemController(ProblemService problemService, ProblemSubmissionService submissionService, ClientService clientService, GraderService graderService) {
        this.problemService = problemService;
        this.submissionService = submissionService;
        this.clientService = clientService;
        this.graderService = graderService;
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<Problem> currentPage = problemService.pageProblems(page, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("programming.list"), new InternalLink(Messages.get("commons.create"), routes.ProgrammingProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index())
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

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            UpsertForm data = form.get();
            Problem problem = problemService.createProblem(data.name, data.gradingEngine, data.additionalNote);

            return redirect(routes.ProgrammingProblemController.viewGeneral(problem.getId()));
        }
    }

    private Result showCreate(Form<UpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("programming.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.create"), routes.ProgrammingProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.viewGeneral(id));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        LazyHtml content = new LazyHtml(viewGeneralView.render(problem));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewGeneral(problemId), routes.ProgrammingProblemController.updateGeneral(problemId), c));
        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.viewGeneral"), routes.ProgrammingProblemController.viewGeneral(problemId))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewStatement(long problemId) {
        String statement = problemService.getStatement(problemId);
        Problem problem = problemService.findProblemById(problemId);

        GradingConfig config = problemService.getGradingConfig(problemId);
        long gradingLastUpdateTime = problemService.getGradingLastUpdateTime(problemId);

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewStatement(routes.ProgrammingProblemController.postSubmit(problemId).absoluteURL(request()), problem.getName(), statement, config, problem.getGradingEngine(), gradingLastUpdateTime));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewStatement(problemId), routes.ProgrammingProblemController.updateStatement(problemId), c));
        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.viewStatement"), routes.ProgrammingProblemController.viewStatement(problemId))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) {
        Problem problem = problemService.findProblemById(problemId);
        Page<ProblemSubmission> submissions = submissionService.pageSubmissions(pageIndex, 20, orderBy, orderDir, IdentityUtils.getUserJid(), problem.getJid());
        LazyHtml content = new LazyHtml(viewSubmissionsView.render(submissions, problemId, pageIndex, orderBy, orderDir));
        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.viewSubmssions"), routes.ProgrammingProblemController.viewSubmissions(problemId, pageIndex, orderBy, orderDir))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewSubmission(long problemId, long submissionId) {
        Problem problem = problemService.findProblemById(problemId);
        Submission submission = submissionService.findSubmissionById(submissionId);

        GradingSource source = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getSubmissionDir(), submission.getJid());

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewSubmission(submission, source));

        appendTabsLayout(content, problemId, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.viewSubmission"), routes.ProgrammingProblemController.viewSubmission(problemId, submissionId))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        UpsertForm content = new UpsertForm();
        content.name = problem.getName();
        content.additionalNote = problem.getAdditionalNote();
        Form<UpsertForm> form = Form.form(UpsertForm.class).fill(content);

        return showUpdateGeneral(form, problem.getId());
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateGeneral(form, problem.getId());
        } else {
            problemService.updateProblem(problem.getId(), form.get().name, form.get().additionalNote);
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
        }
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String statement = problemService.getStatement(problem.getId());
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));

        return showUpdateStatement(form, problem.getId(), problem.getName());
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateStatement(form, problem.getId(), problem.getName());
        } else {
            problemService.updateStatement(problemId, form.get().statement);
            return redirect(routes.ProgrammingProblemController.updateStatement(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateFiles(long id) {
        return redirect(routes.ProgrammingProblemController.updateTestDataFiles(id));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateTestDataFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpdateTestDataFilesForm> form = Form.form(UpdateTestDataFilesForm.class);
        List<File> testDataFiles = problemService.getTestDataFiles(problemId);

        return showUpdateTestDataFiles(form, problem.getId(), problem.getName(), testDataFiles);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateHelperFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpdateHelperFilesForm> form = Form.form(UpdateHelperFilesForm.class);
        List<File> helperFiles = problemService.getHelperFiles(problem.getId());

        return showUpdateHelperFiles(form, problem.getId(), problem.getName(), helperFiles);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateMediaFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpdateMediaFilesForm> form = Form.form(UpdateMediaFilesForm.class);
        List<File> mediaFiles = problemService.getMediaFiles(problem.getId());

        return showUpdateMediaFiles(form, problem.getId(), problem.getName(), mediaFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("testDataFile");
        if (file != null) {
            File testDataFile = file.getFile();
            problemService.uploadTestDataFile(problem.getId(), testDataFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateTestDataFiles(problem.getId()));
        }

        file = body.getFile("testDataFileZipped");
        if (file != null) {
            File testDataFile = file.getFile();
            problemService.uploadTestDataFileZipped(problem.getId(), testDataFile);
            return redirect(routes.ProgrammingProblemController.updateTestDataFiles(problem.getId()));
        }

        file = body.getFile("helperFile");
        if (file != null) {
            File helperFile = file.getFile();
            problemService.uploadHelperFile(problem.getId(), helperFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateHelperFiles(problem.getId()));
        }

        file = body.getFile("helperFileZipped");
        if (file != null) {
            File helperFileZipped = file.getFile();
            problemService.uploadHelperFileZipped(problem.getId(), helperFileZipped);
            return redirect(routes.ProgrammingProblemController.updateHelperFiles(problem.getId()));
        }

        file = body.getFile("mediaFile");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.uploadMediaFile(problem.getId(), mediaFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.updateMediaFiles(problem.getId()));
        }

        file = body.getFile("mediaFileZipped");
        if (file != null) {
            File mediaFileZipped = file.getFile();
            problemService.uploadMediaFileZipped(problem.getId(), mediaFileZipped);
            return redirect(routes.ProgrammingProblemController.updateMediaFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.updateFiles(problem.getId()));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateGrading(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        GradingConfig config = problemService.getGradingConfig(problem.getId());
        List<File> testDataFiles = problemService.getTestDataFiles(problem.getId());
        List<File> helperFiles = problemService.getHelperFiles(problem.getId());

        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createFormFromConfig(config);

        return showUpdateGrading(form, problem, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateGrading(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<?> form = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createFormFromRequest(request());

        if (form.hasErrors() || form.hasGlobalErrors()) {
            List<File> testDataFiles = problemService.getTestDataFiles(problem.getId());
            List<File> helperFiles = problemService.getHelperFiles(problem.getId());
            return showUpdateGrading(form, problem, testDataFiles, helperFiles);
        } else {
            GradingConfig config = GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).createConfigFromForm(form);
            problemService.updateGradingConfig(problemId, config);
            return redirect(routes.ProgrammingProblemController.updateGrading(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();

        String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

        try {
            GradingSource source = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).createGradingSourceFromNewSubmission(body);
            String submissionJid = submissionService.submit(problem.getJid(), problem.getGradingEngine(), gradingLanguage, problem.getTimeUpdate(), source);
            SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).storeSubmissionFiles(SandalphonProperties.getInstance().getSubmissionDir(), submissionJid, source);
        } catch (SubmissionException e) {
            flash("submissionError", e.getMessage());
            return redirect(routes.ProgrammingProblemController.viewStatement(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.viewSubmissions(problem.getId(), 0, "id", "asc"));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result updateClientProblems(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
        List<Client> clients = clientService.findAllClients();

        return showUpdateClientProblems(form, problem, clients, clientProblems);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postUpdateClientProblems(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
            List<Client> clients = clientService.findAllClients();
            return showUpdateClientProblems(form, problem, clients, clientProblems);
        } else {
            ClientProblemUpsertForm clientProblemUpsertForm = form.get();
            if ((clientService.existsByJid(clientProblemUpsertForm.clientJid)) && (!clientService.isClientProblemInProblemByClientJid(problem.getJid(), clientProblemUpsertForm.clientJid))) {
                clientService.createClientProblem(problem.getJid(), clientProblemUpsertForm.clientJid);
                return redirect(routes.ProgrammingProblemController.updateClientProblems(problem.getId()));
            } else {
                List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
                List<Client> clients = clientService.findAllClients();
                return showUpdateClientProblems(form, problem, clients, clientProblems);
            }
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result viewClientProblem(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            LazyHtml content = new LazyHtml(viewClientProblemView.render(problem, clientProblem));
            appendTabsLayout(content, problemId, problem.getName());
            content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                    new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                    new InternalLink(Messages.get("programming.viewClient"), routes.ProgrammingProblemController.viewClientProblem(problemId, clientProblemId))
            ), c));
            appendTemplateLayout(content);

            return lazyOk(content);
        } else {
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result deleteClientProblem(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            clientService.deleteClientProblem(clientProblem.getId());

            return redirect(routes.ProgrammingProblemController.updateClientProblems(problem.getId()));
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
        String clientJid = form.get("clientJid");
        String clientSecret = form.get("clientSecret");
        if (clientService.existsByJid(clientJid)) {
            Client client = clientService.findClientByJid(clientJid);
            if (client.getSecret().equals(clientSecret)) {
                String problemJid = form.get("problemJid");
                if (problemService.problemExistsByJid(problemJid)) {
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
        long gradingLastUpdateTime = problemService.getGradingLastUpdateTime(problem.getId());

        GradingConfig config = problemService.getGradingConfig(problem.getId());

        Html html = SubmissionAdapters.fromGradingEngine(problem.getGradingEngine()).renderViewStatement(postSubmitUri, problem.getName(), statement, config, problem.getGradingEngine(), gradingLastUpdateTime);
        return ok(html);
    }

    public Result downloadGradingFiles() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        ByteArrayOutputStream os = problemService.getZippedGradingFilesStream(problemJid);
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=" + problemJid + ".zip");
        return ok(os.toByteArray()).as("application/zip");
    }

    private Result showUpdateGeneral(Form<UpsertForm> form, long id) {
        LazyHtml content = new LazyHtml(updateGeneralView.render(form, id));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewGeneral(id), routes.ProgrammingProblemController.updateGeneral(id), c));
        appendTabsLayout(content, id, form.get().name);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateGeneral"), routes.ProgrammingProblemController.updateGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long id, String problemName) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, id));
        content.appendLayout(c -> accessTypesLayout.render(routes.ProgrammingProblemController.viewStatement(id), routes.ProgrammingProblemController.updateStatement(id), c));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateStatement"), routes.ProgrammingProblemController.updateStatement(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateGrading(Form<?> form, Problem problem, List<File> testDataFiles, List<File> helperFiles) {
        LazyHtml content = new LazyHtml(GradingConfigAdapters.fromGradingType(problem.getGradingEngine()).renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateGrading"), routes.ProgrammingProblemController.updateGrading(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateTestDataFiles(Form<UpdateTestDataFilesForm> form, long id, String problemName, List<File> testDataFiles) {
        LazyHtml content = new LazyHtml(updateTestDataFilesView.render(form, id, testDataFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateTestDataFiles"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateHelperFiles(Form<UpdateHelperFilesForm> form, long id, String problemName, List<File> helperFiles) {
        LazyHtml content = new LazyHtml(updateHelperFilesView.render(form, id, helperFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateHelperFiles"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateMediaFiles(Form<UpdateMediaFilesForm> form, long id, String problemName, List<File> mediaFiles) {
        LazyHtml content = new LazyHtml(updateMediaFilesView.render(form, id, mediaFiles));
        appendTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.updateMediaFiles"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateClientProblems(Form<ClientProblemUpsertForm> form, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemsView.render(form, problem.getId(), clients, clientProblems));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("programming.clients"), routes.ProgrammingProblemController.updateClientProblems(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }


    private void appendTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programming.general"), routes.ProgrammingProblemController.viewGeneral(id)),
                new InternalLink(Messages.get("programming.statement"), routes.ProgrammingProblemController.viewStatement(id)),
                new InternalLink(Messages.get("programming.grading"), routes.ProgrammingProblemController.updateGrading(id)),
                new InternalLink(Messages.get("programming.files"), routes.ProgrammingProblemController.updateFiles(id)),
                new InternalLink(Messages.get("programming.submissions"), routes.ProgrammingProblemController.viewSubmissions(id, 0, "id", "asc")),
                new InternalLink(Messages.get("programming.clients"), routes.ProgrammingProblemController.updateClientProblems(id))
        ), c));

        content.appendLayout(c -> headingLayout.render("#" + id + ": " + problemName, c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();
        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index()));

        if (SandalphonUtils.hasRole("admin")) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("userRole.userRoles"), routes.UserRoleController.index()));
        }

        content.appendLayout(c -> leftSidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(routes.ProgrammingProblemController.index().absoluteURL(request())).absoluteURL(request()),
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
