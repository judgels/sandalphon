package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ListTableSelectionForm;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.gabriel.commons.Submission;
import org.iatoki.judgels.gabriel.commons.SubmissionAdapters;
import org.iatoki.judgels.gabriel.commons.SubmissionException;
import org.iatoki.judgels.gabriel.commons.SubmissionService;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.programming.submission.listSubmissionsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemSubmissionController extends Controller {
    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;
    private final SubmissionService submissionService;

    public ProgrammingProblemSubmissionController(ProblemService problemService, ProgrammingProblemService programmingProblemService, SubmissionService submissionService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
        this.submissionService = submissionService;
    }

    public Result postSubmit(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem) || !isClean) {
            String engine = programmingProblemService.getGradingEngine(null, problem.getJid());
            Http.MultipartFormData body = request().body().asMultipartFormData();

            String gradingLanguage = body.asFormUrlEncoded().get("language")[0];

            LanguageRestriction languageRestriction = programmingProblemService.getLanguageRestriction(null, problem.getJid());
            Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

            try {
                GradingSource source = SubmissionAdapters.fromGradingEngine(engine).createGradingSourceFromNewSubmission(body);
                String submissionJid = submissionService.submit(problem.getJid(), null, engine, gradingLanguage, allowedLanguageNames, source, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
                SubmissionAdapters.fromGradingEngine(engine).storeSubmissionFiles(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submissionJid, source);
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

    public Result viewSubmissions(long problemId) {
        return listSubmissions(problemId, 0, "id", "desc");
    }

    public Result listSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) {
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

    public Result viewSubmission(long problemId, long submissionId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Submission submission = submissionService.findSubmissionById(submissionId);

            String engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            GradingSource source = SubmissionAdapters.fromGradingEngine(engine).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());

            LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(engine).renderViewSubmission(submission, source, JidCacheService.getInstance().getDisplayName(submission.getAuthorJid()), null, problem.getName(), GradingLanguageRegistry.getInstance().getLanguage(submission.getGradingLanguage()).getName(), null));

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

    public Result regradeSubmission(long problemId, long submissionId, long pageIndex, String orderBy, String orderDir) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Submission submission = submissionService.findSubmissionById(submissionId);
            GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());
            submissionService.regrade(submission.getJid(), source, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

            ControllerUtils.getInstance().addActivityLog("Regrade submission " + submissionId + " of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProgrammingProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
        } else {
            return notFound();
        }
    }

    public Result regradeSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) {
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
                GradingSource source = SubmissionAdapters.fromGradingEngine(submission.getGradingEngine()).createGradingSourceFromPastSubmission(SandalphonProperties.getInstance().getBaseSubmissionsDir(), submission.getJid());
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
