package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ListTableSelectionForm;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.commons.SubmissionService;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.gabriel.commons.Submission;
import org.iatoki.judgels.gabriel.commons.SubmissionException;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.gabriel.commons.SubmissionAdapters;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.commons.programming.ProgrammingProblem;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapter;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithAutoPopulation;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithTokilibFormat;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.forms.programming.ProgrammingUpsertForm;
import org.iatoki.judgels.sandalphon.views.html.listView;
import org.iatoki.judgels.sandalphon.views.html.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.viewClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listGradingTestDataFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.listGradingHelperFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.listSubmissionsView;
import org.iatoki.judgels.sandalphon.views.html.programming.configs.autoPopulationLayout;
import org.iatoki.judgels.sandalphon.views.html.programming.configs.tokilibLayout;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
public final class ProgrammingProblemController extends Controller {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final ClientService clientService;
    private final ProgrammingProblemService programmingProblemService;
    private final SubmissionService submissionService;
    private final GraderService graderService;

    public ProgrammingProblemController(ProblemService problemService, ClientService clientService, ProgrammingProblemService programmingProblemService, SubmissionService submissionService, GraderService graderService) {
        this.problemService = problemService;
        this.clientService = clientService;
        this.programmingProblemService = programmingProblemService;
        this.submissionService = submissionService;
        this.graderService = graderService;

        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result create() {
        Form<ProgrammingUpsertForm> form = Form.form(ProgrammingUpsertForm.class);
        return showCreate(form);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postCreate() {
        Form<ProgrammingUpsertForm> form = Form.form(ProgrammingUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            ProgrammingUpsertForm data = form.get();

            LanguageRestriction languageRestriction = LanguageRestrictionAdapter.createLanguageRestrictionFromForm(data.allowedLanguageNames, data.isAllowedAll);
            ProgrammingProblem programmingProblem = programmingProblemService.createProgrammingProblem(data.gradingEngine, data.additionalNote, languageRestriction);

            Problem problem = problemService.createProblem(data.name, programmingProblem.getJid());

            return redirect(routes.ProgrammingProblemController.viewStatement(problem.getId()));
        }
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());

        String statement = problemService.getStatement(problem.getJid());
        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(programmingProblem.getLanguageRestriction()));

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).renderViewStatement(routes.ProgrammingProblemController.postSubmit(problemId).absoluteURL(request()), problem.getName(), statement, config, programmingProblem.getGradingEngine(), allowedLanguageNames));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProgrammingProblemController.viewStatement(problemId)),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problemId))
        ), c));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.view"), routes.ProgrammingProblemController.viewStatement(problemId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }



    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid(), problem);
        LazyHtml content = new LazyHtml(viewGeneralView.render(programmingProblem));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProgrammingProblemController.viewGeneral(problemId)),
                new InternalLink(Messages.get("commons.update"), routes.ProgrammingProblemController.updateGeneral(problemId))
                ), c));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.general"), routes.ProblemController.jumpToGeneral(problemId)),
                new InternalLink(Messages.get("problem.general.view"), routes.ProgrammingProblemController.viewGeneral(problemId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());
        ProgrammingUpsertForm data = new ProgrammingUpsertForm();
        data.name = problem.getName();
        data.gradingEngine = programmingProblem.getGradingEngine();
        data.additionalNote = programmingProblem.getAdditionalNote();
        data.allowedLanguageNames = LanguageRestrictionAdapter.getFormAllowedLanguageNamesFromLanguageRestriction(programmingProblem.getLanguageRestriction());
        data.isAllowedAll = LanguageRestrictionAdapter.getFormIsAllowedAllFromLanguageRestriction(programmingProblem.getLanguageRestriction());
        Form<ProgrammingUpsertForm> form = Form.form(ProgrammingUpsertForm.class).fill(data);

        return showUpdateGeneral(form, problem);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateGeneral(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ProgrammingUpsertForm> form = Form.form(ProgrammingUpsertForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateGeneral(form, problem);
        } else {
            ProgrammingUpsertForm data = form.get();
            LanguageRestriction languageRestriction = LanguageRestrictionAdapter.createLanguageRestrictionFromForm(data.allowedLanguageNames, data.isAllowedAll);
            problemService.updateProblem(problem.getId(), data.name);
            programmingProblemService.updateProgrammingProblem(problem.getJid(), data.gradingEngine, data.additionalNote, languageRestriction);
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewSubmissions(long problemId) {
        return listSubmissions(problemId, 0, "id", "desc");
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) {
        Problem problem = problemService.findProblemById(problemId);

        Page<Submission> submissions = submissionService.pageSubmissions(pageIndex, PAGE_SIZE, orderBy, orderDir, null, problem.getJid(), null);
        Map<String, String> gradingLanguageToNameMap = GradingLanguageRegistry.getInstance().getGradingLanguages();

        LazyHtml content = new LazyHtml(listSubmissionsView.render(submissions, gradingLanguageToNameMap, problemId, pageIndex, orderBy, orderDir));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problemId)),
                new InternalLink(Messages.get("problem.programming.submission.list"), routes.ProgrammingProblemController.viewSubmissions(problemId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewSubmission(long problemId, long submissionId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());
        Submission submission = submissionService.findSubmissionById(submissionId);

        GradingSource source = SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseProgrammingSubmissionDir(), submission.getJid());

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).renderViewSubmission(submission, source, JidCacheService.getInstance().getDisplayName(submission.getAuthorJid()), null, problem.getName(), GradingLanguageRegistry.getInstance().getLanguage(submission.getGradingLanguage()).getName(), null));

        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problemId)),
                new InternalLink(Messages.get("problem.programming.submission.view"), routes.ProgrammingProblemController.viewSubmission(problemId, submissionId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result regradeSubmission(long problemId, long submissionId, long pageIndex, String orderBy, String orderDir) {
        Submission submission = submissionService.findSubmissionById(submissionId);
        GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseProgrammingSubmissionDir(), submission.getJid());
        submissionService.regrade(submission.getJid(), source);

        return redirect(routes.ProgrammingProblemController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result regradeSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) {
        ListTableSelectionForm data = Form.form(ListTableSelectionForm.class).bindFromRequest().get();

        Problem problem = problemService.findProblemById(problemId);

        List<Submission> submissions;

        if (data.selectAll) {
            submissions = submissionService.findSubmissionsWithoutGradingsByFilters(orderBy, orderDir, null, problem.getJid(), null);
        } else if (data.selectJids != null) {
            submissions = submissionService.findSubmissionsWithoutGradingsByJids(data.selectJids);
        } else {
            return redirect(routes.ProgrammingProblemController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
        }

        for (Submission submission : submissions) {
            GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseProgrammingSubmissionDir(), submission.getJid());
            submissionService.regrade(submission.getJid(), source);
        }

        return redirect(routes.ProgrammingProblemController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToGrading(long id) {
        return redirect(routes.ProgrammingProblemController.updateGradingConfig(id));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToSubmissions(long id) {
        return redirect(routes.ProgrammingProblemController.viewSubmissions(id));
    }


    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGradingConfig(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid(), problem);
        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        List<File> testDataFiles = programmingProblemService.getTestDataFiles(problem.getJid());
        List<File> helperFiles = programmingProblemService.getHelperFiles(problem.getJid());

        Form<?> form = GradingConfigAdapters.fromGradingType(programmingProblem.getGradingEngine()).createFormFromConfig(config);

        return showUpdateGradingConfig(form, programmingProblem, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateGradingConfig(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid(), problem);
        Form<?> form = GradingConfigAdapters.fromGradingType(programmingProblem.getGradingEngine()).createEmptyForm().bindFromRequest(request());

        if (form.hasErrors() || form.hasGlobalErrors()) {
            List<File> testDataFiles = programmingProblemService.getTestDataFiles(problem.getJid());
            List<File> helperFiles = programmingProblemService.getHelperFiles(problem.getJid());
            return showUpdateGradingConfig(form, programmingProblem, testDataFiles, helperFiles);
        } else {
            GradingConfig config = GradingConfigAdapters.fromGradingType(programmingProblem.getGradingEngine()).createConfigFromForm(form);
            programmingProblemService.updateGradingConfig(problem.getJid(), config);
            return redirect(routes.ProgrammingProblemController.updateGradingConfig(problem.getId()));
        }
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGradingConfigByTokilibFormat(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());
        List<File> testDataFiles = programmingProblemService.getTestDataFiles(problem.getJid());
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(programmingProblem.getGradingEngine());

        if (! (adapter instanceof ConfigurableWithTokilibFormat)) {
            return forbidden();
        }

        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        GradingConfig newConfig = ((ConfigurableWithTokilibFormat) adapter).updateConfigWithTokilibFormat(config, testDataFiles);

        programmingProblemService.updateGradingConfig(problem.getJid(), newConfig);

        return redirect(routes.ProgrammingProblemController.updateGradingConfig(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGradingConfigByAutoPopulation(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());
        List<File> testDataFiles = programmingProblemService.getTestDataFiles(problem.getJid());
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(programmingProblem.getGradingEngine());

        if (! (adapter instanceof ConfigurableWithAutoPopulation)) {
            return forbidden();
        }

        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        GradingConfig newConfig = ((ConfigurableWithAutoPopulation) adapter).updateConfigWithAutoPopulation(config, testDataFiles);

        programmingProblemService.updateGradingConfig(problem.getJid(), newConfig);

        return redirect(routes.ProgrammingProblemController.updateGradingConfig(problem.getId()));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listGradingTestDataFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UploadFileForm> form = Form.form(UploadFileForm.class);
        List<File> testDataFiles = programmingProblemService.getTestDataFiles(problem.getJid());

        return showListGradingTestDataFiles(form, problem, testDataFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUploadGradingTestDataFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            File testDataFile = file.getFile();
            programmingProblemService.uploadTestDataFile(problem.getJid(), testDataFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File testDataFile = file.getFile();
            programmingProblemService.uploadTestDataFileZipped(problem.getJid(), testDataFile);
            return redirect(routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listGradingHelperFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UploadFileForm> form = Form.form(UploadFileForm.class);
        List<File> helperFiles = programmingProblemService.getHelperFiles(problem.getJid());

        return showListGradingHelperFiles(form, problem, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUploadGradingHelperFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            File helperFile = file.getFile();
            programmingProblemService.uploadHelperFile(problem.getJid(), helperFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File helperFile = file.getFile();
            programmingProblemService.uploadHelperFileZipped(problem.getJid(), helperFile);
            return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problem.getJid());
        Http.MultipartFormData body = request().body().asMultipartFormData();

        String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(programmingProblem.getLanguageRestriction()));

        try {
            GradingSource source = SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).createGradingSourceFromNewSubmission(body);
            String submissionJid = submissionService.submit(problem.getJid(), null, programmingProblem.getGradingEngine(), gradingLanguage, allowedLanguageNames, source);
            SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).storeSubmissionFiles(SandalphonProperties.getInstance().getBaseProgrammingSubmissionDir(), submissionJid, source);
        } catch (SubmissionException e) {
            flash("submissionError", e.getMessage());
            return redirect(routes.ProgrammingProblemController.viewStatement(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.viewSubmissions(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result downloadGradingTestDataFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        File file = programmingProblemService.getTestDataFile(problem.getJid(), filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return notFound();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result downloadGradingHelperFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        File file = programmingProblemService.getHelperFile(problem.getJid(), filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return notFound();
        }
    }

    public Result viewProblemStatementTOTP(String clientJid, String problemJid, int TOTP, String lang, String postSubmitUri) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        if (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid)) {
            return notFound();
        }

        LanguageRestriction languageRestriction = new Gson().fromJson(request().body().asText(), LanguageRestriction.class);

        if (languageRestriction == null) {
            return badRequest();
        }

        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        if (!googleAuthenticator.authorize(new Base32().encodeAsString(clientProblem.getSecret().getBytes()), TOTP)) {
            return forbidden();
        }

        String statement = problemService.getStatement(problemJid);
        Problem problem = problemService.findProblemByJid(problemJid);
        ProgrammingProblem programmingProblem = programmingProblemService.findProgrammingProblemByJid(problemJid);
        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(programmingProblem.getLanguageRestriction(), languageRestriction));

        GradingConfig config = programmingProblemService.getGradingConfig(programmingProblem.getJid());

        Html html = SubmissionAdapters.fromGradingEngine(programmingProblem.getGradingEngine()).renderViewStatement(postSubmitUri, problem.getName(), statement, config, programmingProblem.getGradingEngine(), allowedLanguageNames);
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

        ByteArrayOutputStream os = programmingProblemService.getZippedGradingFilesStream(problemJid);
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=" + problemJid + ".zip");
        return ok(os.toByteArray()).as("application/zip");
    }

    public Result getGradingLastUpdateTime() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        Date gradingLastUpdateTime = programmingProblemService.getGradingLastUpdateTime(problemJid);

        return ok("" + gradingLastUpdateTime.getTime());
    }

    private Result downloadFile(File file) {
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
        return ok(file);
    }

    private Result showCreate(Form<ProgrammingUpsertForm> form) {
        GradingLanguageRegistry.getInstance().getGradingLanguages();
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.programming.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.create"), routes.ProgrammingProblemController.create())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateGeneral(Form<ProgrammingUpsertForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateGeneralView.render(form, problem.getId()));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProgrammingProblemController.viewGeneral(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProgrammingProblemController.updateGeneral(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.general"), routes.ProblemController.jumpToGeneral(problem.getId())),
                new InternalLink(Messages.get("problem.general.update"), routes.ProgrammingProblemController.updateGeneral(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateGradingConfig(Form<?> form, ProgrammingProblem problem, List<File> testDataFiles, List<File> helperFiles) {
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(problem.getGradingEngine());
        LazyHtml content = new LazyHtml(adapter.renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));

        if (adapter instanceof ConfigurableWithTokilibFormat) {
            content.appendLayout(c -> tokilibLayout.render(problem.getId(), c));
        } else if (adapter instanceof ConfigurableWithAutoPopulation) {
            content.appendLayout(c -> autoPopulationLayout.render(problem.getId(), c));
        }

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()))
        ), c));

        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config.update"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingTestDataFiles(Form<UploadFileForm> form, Problem problem, List<File> testDataFiles) {
        LazyHtml content = new LazyHtml(listGradingTestDataFilesView.render(form, problem.getId(), testDataFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData.list"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingHelperFiles(Form<UploadFileForm> form, Problem problem, List<File> helperFiles) {
        LazyHtml content = new LazyHtml(listGradingHelperFilesView.render(form, problem.getId(), helperFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper.list"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
