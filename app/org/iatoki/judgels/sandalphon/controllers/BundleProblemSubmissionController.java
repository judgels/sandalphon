package org.iatoki.judgels.sandalphon.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ListTableSelectionForm;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.sandalphon.services.JidCacheService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleSubmission;
import org.iatoki.judgels.sandalphon.BundleSubmissionNotFoundException;
import org.iatoki.judgels.sandalphon.services.BundleSubmissionService;
import org.iatoki.judgels.sandalphon.views.html.bundleSubmissionView;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.bundle.submission.listSubmissionsView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class BundleProblemSubmissionController extends BaseController {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;
    private final BundleSubmissionService submissionService;
    private final FileSystemProvider submissionFileProvider;

    public BundleProblemSubmissionController(ProblemService problemService, BundleProblemService bundleProblemService, BundleSubmissionService submissionService, FileSystemProvider submissionFileProvider) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
        this.submissionService = submissionService;
        this.submissionFileProvider = submissionFileProvider;
    }

    @Transactional
    public Result postSubmit(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        if (BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem) || !isClean) {
            DynamicForm data = Form.form().bindFromRequest();

            BundleAnswer answer = submissionService.createBundleAnswerFromNewSubmission(data, ProblemControllerUtils.getCurrentStatementLanguage());
            String submissionJid = submissionService.submit(problem.getJid(), null, answer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
            submissionService.storeSubmissionFiles(submissionFileProvider, null, submissionJid, answer);

            ControllerUtils.getInstance().addActivityLog("Submit to bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.BundleProblemSubmissionController.viewSubmissions(problem.getId()));
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

        if (BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            Page<BundleSubmission> submissions = submissionService.pageSubmissions(pageIndex, PAGE_SIZE, orderBy, orderDir, null, problem.getJid(), null);

            LazyHtml content = new LazyHtml(listSubmissionsView.render(submissions, problemId, pageIndex, orderBy, orderDir));
            BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.bundle.submission.list"), routes.BundleProblemSubmissionController.viewSubmissions(problemId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Submissions");

            ControllerUtils.getInstance().addActivityLog("List submissions of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result viewSubmission(long problemId, long submissionId) throws ProblemNotFoundException, BundleSubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            try {
                BundleSubmission submission = submissionService.findSubmissionById(submissionId);
                BundleAnswer answer = submissionService.createBundleAnswerFromPastSubmission(submissionFileProvider, null, submission.getJid());

                LazyHtml content = new LazyHtml(bundleSubmissionView.render(submission, new Gson().fromJson(submission.getLatestDetails(), new TypeToken<Map<String, Double>>(){}.getType()), answer, JidCacheService.getInstance().getDisplayName(submission.getAuthorJid()), null, problem.getName(), null));

                BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
                ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
                ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
                ControllerUtils.getInstance().appendSidebarLayout(content);
                appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.submission.view"), routes.ProgrammingProblemSubmissionController.viewSubmission(problemId, submissionId)));
                ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View Submission");

                ControllerUtils.getInstance().addActivityLog("View submission " + submissionId + " of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return ControllerUtils.getInstance().lazyOk(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    @Transactional
    public Result regradeSubmission(long problemId, long submissionId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException, BundleSubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        try {
            if (BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
                BundleSubmission submission = submissionService.findSubmissionById(submissionId);
                BundleAnswer answer = submissionService.createBundleAnswerFromPastSubmission(submissionFileProvider, null, submission.getJid());
                submissionService.regrade(submission.getJid(), answer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

                ControllerUtils.getInstance().addActivityLog("Regrade submission " + submissionId + " of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
            } else {
                return notFound();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Result regradeSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            ListTableSelectionForm data = Form.form(ListTableSelectionForm.class).bindFromRequest().get();

            List<BundleSubmission> submissions;

            if (data.selectAll) {
                submissions = submissionService.findSubmissionsWithoutGradingsByFilters(orderBy, orderDir, null, problem.getJid(), null);
            } else if (data.selectJids != null) {
                submissions = submissionService.findSubmissionsWithoutGradingsByJids(data.selectJids);
            } else {
                return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
            }

            try {
                for (BundleSubmission submission : submissions) {
                    BundleAnswer answer = submissionService.createBundleAnswerFromPastSubmission(submissionFileProvider, null, submission.getJid());
                    submissionService.regrade(submission.getJid(), answer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
                }

                ControllerUtils.getInstance().addActivityLog("Regrade submissions of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.bundle.submission"), routes.BundleProblemController.jumpToSubmissions(problem.getId())))
                        .add(lastLink)
                .build()
        );
    }
}
