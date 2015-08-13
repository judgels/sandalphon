package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.forms.ListTableSelectionForm;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.Submission;
import org.iatoki.judgels.sandalphon.SubmissionException;
import org.iatoki.judgels.sandalphon.SubmissionNotFoundException;
import org.iatoki.judgels.sandalphon.adapters.impls.SubmissionAdapterRegistry;
import org.iatoki.judgels.sandalphon.config.SubmissionFile;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.services.SubmissionService;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.submission.listSubmissionsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProgrammingProblemSubmissionController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;
    private final SubmissionService submissionService;
    private final FileSystemProvider submissionFileSystemProvider;

    @Inject
    public ProgrammingProblemSubmissionController(ProblemService problemService, ProgrammingProblemService programmingProblemService, SubmissionService submissionService, @SubmissionFile FileSystemProvider submissionFileSystemProvider) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
        this.submissionService = submissionService;
        this.submissionFileSystemProvider = submissionFileSystemProvider;
    }

    @Transactional
    public Result postSubmit(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem) || !isClean) {
            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(null, problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }
            Http.MultipartFormData body = request().body().asMultipartFormData();

            String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

            LanguageRestriction languageRestriction;
            try {
                languageRestriction = programmingProblemService.getLanguageRestriction(null, problem.getJid());
            } catch (IOException e) {
                languageRestriction = LanguageRestriction.defaultRestriction();
            }
            Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

            try {
                GradingSource source = SubmissionAdapterRegistry.getInstance().getByGradingEngineName(engine).createGradingSourceFromNewSubmission(body);
                String submissionJid = submissionService.submit(problem.getJid(), null, engine, gradingLanguage, allowedLanguageNames, source, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
                SubmissionAdapterRegistry.getInstance().getByGradingEngineName(engine).storeSubmissionFiles(submissionFileSystemProvider, null, submissionJid, source);
            } catch (SubmissionException e) {
                flash("submissionError", e.getMessage());
                return redirect(routes.ProgrammingProblemStatementController.viewStatement(problem.getId()));
            }

            ControllerUtils.getInstance().addActivityLog("Submit to programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProgrammingProblemSubmissionController.viewSubmissions(problem.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result viewSubmissions(long problemId) throws ProblemNotFoundException  {
        return listSubmissions(problemId, 0, "id", "desc");
    }

    @Transactional(readOnly = true)
    public Result listSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Page<Submission> submissions = submissionService.pageSubmissions(pageIndex, PAGE_SIZE, orderBy, orderDir, null, problem.getJid(), null);
            Map<String, String> gradingLanguageToNameMap = GradingLanguageRegistry.getInstance().getGradingLanguages();

            LazyHtml content = new LazyHtml(listSubmissionsView.render(submissions, gradingLanguageToNameMap, problemId, pageIndex, orderBy, orderDir));
            ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.submission.list"), routes.ProgrammingProblemSubmissionController.viewSubmissions(problemId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Submissions");

            ControllerUtils.getInstance().addActivityLog("List submissions of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result viewSubmission(long problemId, long submissionId) throws ProblemNotFoundException, SubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Submission submission = submissionService.findSubmissionById(submissionId);

            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }
            GradingSource source = SubmissionAdapterRegistry.getInstance().getByGradingEngineName(engine).createGradingSourceFromPastSubmission(submissionFileSystemProvider, null, submission.getJid());

            LazyHtml content = new LazyHtml(SubmissionAdapterRegistry.getInstance().getByGradingEngineName(engine).renderViewSubmission(submission, source, JidCacheServiceImpl.getInstance().getDisplayName(submission.getAuthorJid()), null, problem.getName(), GradingLanguageRegistry.getInstance().getLanguage(submission.getGradingLanguage()).getName(), null));

            ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.submission.view"), routes.ProgrammingProblemSubmissionController.viewSubmission(problemId, submissionId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View Submission");

            ControllerUtils.getInstance().addActivityLog("View submission " + submissionId + " of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    @Transactional
    public Result regradeSubmission(long problemId, long submissionId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException, SubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Submission submission = submissionService.findSubmissionById(submissionId);
            GradingSource source = SubmissionAdapterRegistry.getInstance().getByGradingEngineName(submission.getGradingEngine()).createGradingSourceFromPastSubmission(submissionFileSystemProvider, null, submission.getJid());
            submissionService.regrade(submission.getJid(), source, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

            ControllerUtils.getInstance().addActivityLog("Regrade submission " + submissionId + " of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProgrammingProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
        } else {
            return notFound();
        }
    }

    @Transactional
    public Result regradeSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            ListTableSelectionForm data = Form.form(ListTableSelectionForm.class).bindFromRequest().get();

            List<Submission> submissions;

            if (data.selectAll) {
                submissions = submissionService.findSubmissionsWithoutGradingsByFilters(orderBy, orderDir, null, problem.getJid(), null);
            } else if (data.selectJids != null) {
                submissions = submissionService.findSubmissionsWithoutGradingsByJids(data.selectJids);
            } else {
                return redirect(routes.ProgrammingProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
            }

            for (Submission submission : submissions) {
                GradingSource source = SubmissionAdapterRegistry.getInstance().getByGradingEngineName(submission.getGradingEngine()).createGradingSourceFromPastSubmission(submissionFileSystemProvider, null, submission.getJid());
                submissionService.regrade(submission.getJid(), source, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
            }

            ControllerUtils.getInstance().addActivityLog("Regrade submissions of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProgrammingProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
        } else {
            return notFound();
        }
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problem.getId())))
                        .add(lastLink)
                .build()
        );
    }
}
