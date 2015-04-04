package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ListTableSelectionForm;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.commons.SubmissionService;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.gabriel.commons.Submission;
import org.iatoki.judgels.gabriel.commons.SubmissionException;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.gabriel.commons.SubmissionAdapters;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.commons.WorldLanguageRegistry;
import org.iatoki.judgels.sandalphon.forms.programming.GradingEngineUpdateForm;
import org.iatoki.judgels.sandalphon.forms.programming.LanguageRestrictionUpdateForm;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapter;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemStatementUtils;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithAutoPopulation;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithTokilibFormat;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.forms.programming.ProgrammingProblemCreateForm;
import org.iatoki.judgels.sandalphon.views.html.programming.createProgrammingProblemView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGradingEngineView;
import org.iatoki.judgels.sandalphon.views.html.programming.listGradingTestDataFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.listGradingHelperFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateLanguageRestrictionView;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Result createProgrammingProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        Form<ProgrammingProblemCreateForm> form = Form.form(ProgrammingProblemCreateForm.class);
        return showCreateProgrammingProblem(form);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postCreateProgrammingProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        Form<ProgrammingProblemCreateForm> form = Form.form(ProgrammingProblemCreateForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreateProgrammingProblem(form);
        } else {
            ProgrammingProblemCreateForm data = form.get();

            Problem problem = problemService.createProblem(ProblemType.PROGRAMMING, ProblemControllerUtils.getJustCreatedProblemName(), ProblemControllerUtils.getJustCreatedProblemAdditionalNote(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            problemService.updateStatement(problem.getId(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode(), ProgrammingProblemStatementUtils.getDefaultStatement(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode()));
            programmingProblemService.initProgrammingProblem(problem.getJid(), data.gradingEngineName);

            ProblemControllerUtils.setCurrentStatementLanguage(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            ProblemControllerUtils.removeJustCreatedProblem();
            return redirect(routes.ProblemController.enterProblem(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewStatement(long problemId) {
        ProblemControllerUtils.establishStatementLanguage(problemService, problemId);

        Problem problem = problemService.findProblemById(problemId);

        String statement = problemService.getStatement(problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());

        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        LanguageRestriction languageRestriction = programmingProblemService.getLanguageRestriction(problem.getJid());
        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(engine).renderViewStatement(routes.ProgrammingProblemController.postSubmit(problemId).absoluteURL(request()), problem.getName(), statement, config, engine, allowedLanguageNames));

        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(problem.getJid());
        List<String> allowedLanguages = availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toList());

        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemController.viewStatementSwitchLanguage(problem.getId()));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProgrammingProblemController.viewStatement(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language"), routes.ProblemController.listStatementLanguages(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.view"), routes.ProgrammingProblemController.viewStatement(problemId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
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
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
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
        Submission submission = submissionService.findSubmissionById(submissionId);

        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        GradingSource source = SubmissionAdapters.fromGradingEngine(engine).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());

        LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(engine).renderViewSubmission(submission, source, JidCacheService.getInstance().getDisplayName(submission.getAuthorJid()), null, problem.getName(), GradingLanguageRegistry.getInstance().getLanguage(submission.getGradingLanguage()).getName(), null));

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
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
        GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());
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
            GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());
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
    public Result updateGradingEngine(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        GradingEngineUpdateForm data = new GradingEngineUpdateForm();
        data.gradingEngineName = programmingProblemService.getGradingEngine(problem.getJid());

        Form<GradingEngineUpdateForm> form = Form.form(GradingEngineUpdateForm.class).fill(data);

        return showUpdateGradingEngine(form, problem);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateGradingEngine(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Form<GradingEngineUpdateForm> form = Form.form(GradingEngineUpdateForm.class).bindFromRequest(request());

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateGradingEngine(form, problem);
        } else {
            String gradingEngine = form.get().gradingEngineName;
            String originalGradingEngine = programmingProblemService.getGradingEngine(problem.getJid());

            if (!gradingEngine.equals(originalGradingEngine)) {
                GradingConfig config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
                programmingProblemService.updateGradingConfig(problem.getJid(), config);
            }

            programmingProblemService.updateGradingEngine(problem.getJid(), gradingEngine);
            return redirect(routes.ProgrammingProblemController.updateGradingConfig(problem.getId()));
        }
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGradingConfig(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(problem.getJid());
        List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(problem.getJid());

        Form<?> form = GradingConfigAdapters.fromGradingType(engine).createFormFromConfig(config);

        return showUpdateGradingConfig(form, problem, engine, testDataFiles, helperFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateGradingConfig(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        Form<?> form = GradingConfigAdapters.fromGradingType(engine).createEmptyForm().bindFromRequest(request());

        if (form.hasErrors() || form.hasGlobalErrors()) {
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(problem.getJid());
            List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(problem.getJid());
            return showUpdateGradingConfig(form, problem, engine, testDataFiles, helperFiles);
        } else {
            GradingConfig config = GradingConfigAdapters.fromGradingType(engine).createConfigFromForm(form);
            programmingProblemService.updateGradingConfig(problem.getJid(), config);
            return redirect(routes.ProgrammingProblemController.updateGradingConfig(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateGradingConfigByTokilibFormat(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(problem.getJid());
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(engine);

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
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(problem.getJid());
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(engine);

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
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(problem.getJid());

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
            programmingProblemService.uploadGradingTestDataFile(problem.getJid(), testDataFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File testDataFile = file.getFile();
            programmingProblemService.uploadGradingTestDataFileZipped(problem.getJid(), testDataFile);
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
        List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(problem.getJid());

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
            programmingProblemService.uploadGradingHelperFile(problem.getJid(), helperFile, file.getFilename());
            return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File helperFile = file.getFile();
            programmingProblemService.uploadGradingHelperFileZipped(problem.getJid(), helperFile);
            return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()));
    }


    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateLanguageRestriction(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        LanguageRestriction languageRestriction = programmingProblemService.getLanguageRestriction(problem.getJid());

        LanguageRestrictionUpdateForm data = new LanguageRestrictionUpdateForm();
        data.allowedLanguageNames = LanguageRestrictionAdapter.getFormAllowedLanguageNamesFromLanguageRestriction(languageRestriction);
        data.isAllowedAll = LanguageRestrictionAdapter.getFormIsAllowedAllFromLanguageRestriction(languageRestriction);

        Form<LanguageRestrictionUpdateForm> form = Form.form(LanguageRestrictionUpdateForm.class).fill(data);

        return showUpdateLanguageRestriction(form, problem);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateLanguageRestriction(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Form<LanguageRestrictionUpdateForm> form = Form.form(LanguageRestrictionUpdateForm.class).bindFromRequest(request());

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateLanguageRestriction(form, problem);
        } else {
            LanguageRestrictionUpdateForm data = form.get();
            LanguageRestriction languageRestriction = LanguageRestrictionAdapter.createLanguageRestrictionFromForm(data.allowedLanguageNames, data.isAllowedAll);

            programmingProblemService.updateLanguageRestriction(problem.getJid(), languageRestriction);
            return redirect(routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        Http.MultipartFormData body = request().body().asMultipartFormData();

        String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

        LanguageRestriction languageRestriction = programmingProblemService.getLanguageRestriction(problem.getJid());
        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

        try {
            GradingSource source = SubmissionAdapters.fromGradingEngine(engine).createGradingSourceFromNewSubmission(body);
            String submissionJid = submissionService.submit(problem.getJid(), null, engine, gradingLanguage, allowedLanguageNames, source);
            SubmissionAdapters.fromGradingEngine(engine).storeSubmissionFiles(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submissionJid, source);
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
        String testDataURL = programmingProblemService.getGradingTestDataFileURL(problem.getJid(), filename);
        try {
            new URL(testDataURL);
            return redirect(testDataURL);
        } catch (MalformedURLException e) {
            File testDataFile = new File(testDataURL);
            return downloadFile(testDataFile);
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result downloadGradingHelperFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        String helperURL = programmingProblemService.getGradingHelperFileURL(problem.getJid(), filename);
        try {
            new URL(helperURL);
            return redirect(helperURL);
        } catch (MalformedURLException e) {
            File helperFile = new File(helperURL);
            return downloadFile(helperFile);
        }
    }

    public Result viewProblemStatementTOTP(String clientJid, String problemJid, int TOTP, String lang, String postSubmitUri, String switchLanguageUri) {
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

        Problem problem = problemService.findProblemByJid(problemJid);
        String engine = programmingProblemService.getGradingEngine(problem.getJid());
        LanguageRestriction problemLanguageRestriction = programmingProblemService.getLanguageRestriction(problem.getJid());
        Set<String> allowedGradingLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(problemLanguageRestriction, languageRestriction));

        GradingConfig config = programmingProblemService.getGradingConfig(problem.getJid());

        Map<String, StatementLanguageStatus> availableStatementLanguages = problemService.getAvailableLanguages(problem.getJid());

        if (!availableStatementLanguages.containsKey(lang) || availableStatementLanguages.get(lang) == StatementLanguageStatus.DISABLED) {
            lang = problemService.getDefaultLanguage(problemJid);
        }

        String statement = problemService.getStatement(problemJid, lang);

        List<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toList());

        Html html = SubmissionAdapters.fromGradingEngine(engine).renderViewStatement(postSubmitUri, problem.getName(), statement, config, engine, allowedGradingLanguageNames);
        html = SubmissionAdapters.fromGradingEngine(engine).renderStatementLanguageSelection(switchLanguageUri, allowedStatementLanguages, lang, html);
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

    private Result showCreateProgrammingProblem(Form<ProgrammingProblemCreateForm> form) {
        LazyHtml content = new LazyHtml(createProgrammingProblemView.render(form, ProblemControllerUtils.getJustCreatedProblemName(), ProblemControllerUtils.getJustCreatedProblemAdditionalNote(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode()));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.programming.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Programming Problem - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateGradingEngine(Form<GradingEngineUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateGradingEngineView.render(form, problem));

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ), c));

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.engine.update"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Grading Engine");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateGradingConfig(Form<?> form, Problem problem, String gradingEngine, List<FileInfo> testDataFiles, List<FileInfo> helperFiles) {
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(gradingEngine);
        LazyHtml content = new LazyHtml(adapter.renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));

        if (adapter instanceof ConfigurableWithTokilibFormat) {
            content.appendLayout(c -> tokilibLayout.render(problem.getId(), c));
        } else if (adapter instanceof ConfigurableWithAutoPopulation) {
            content.appendLayout(c -> autoPopulationLayout.render(problem.getId(), c));
        }

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ), c));

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config.update"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingTestDataFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> testDataFiles) {
        LazyHtml content = new LazyHtml(listGradingTestDataFilesView.render(form, problem.getId(), testDataFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData.list"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingHelperFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> helperFiles) {
        LazyHtml content = new LazyHtml(listGradingHelperFilesView.render(form, problem.getId(), helperFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper.list"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateLanguageRestriction(Form<LanguageRestrictionUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateLanguageRestrictionView.render(form, problem));

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ), c));

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction.update"), routes.ProgrammingProblemController.updateLanguageRestriction(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Language Restriction");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
