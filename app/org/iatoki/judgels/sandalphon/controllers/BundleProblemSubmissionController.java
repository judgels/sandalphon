package org.iatoki.judgels.sandalphon.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.forms.ListTableSelectionForm;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleDetailResult;
import org.iatoki.judgels.sandalphon.BundleSubmission;
import org.iatoki.judgels.sandalphon.BundleSubmissionNotFoundException;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.config.SubmissionFileSystemProvider;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.BundleSubmissionService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.submission.listSubmissionsView;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.submission.bundleSubmissionView;
import play.data.DynamicForm;
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

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class BundleProblemSubmissionController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final FileSystemProvider bundleSubmissionFileSystemProvider;
    private final BundleSubmissionService bundleSubmissionService;
    private final ProblemService problemService;

    @Inject
    public BundleProblemSubmissionController(@SubmissionFileSystemProvider FileSystemProvider bundleSubmissionFileSystemProvider, BundleSubmissionService bundleSubmissionService, ProblemService problemService) {
        this.bundleSubmissionFileSystemProvider = bundleSubmissionFileSystemProvider;
        this.bundleSubmissionService = bundleSubmissionService;
        this.problemService = problemService;
    }

    @Transactional
    public Result postSubmit(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        if (!BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem) && isClean) {
            return notFound();
        }

        DynamicForm dForm = Form.form().bindFromRequest();

        BundleAnswer bundleAnswer = bundleSubmissionService.createBundleAnswerFromNewSubmission(dForm, ProblemControllerUtils.getCurrentStatementLanguage());
        String submissionJid = bundleSubmissionService.submit(problem.getJid(), null, bundleAnswer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
        bundleSubmissionService.storeSubmissionFiles(bundleSubmissionFileSystemProvider, null, submissionJid, bundleAnswer);

        ControllerUtils.getInstance().addActivityLog("Submit to bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleProblemSubmissionController.viewSubmissions(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result viewSubmissions(long problemId) throws ProblemNotFoundException  {
        return listSubmissions(problemId, 0, "id", "desc");
    }

    @Transactional(readOnly = true)
    public Result listSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            return notFound();
        }

        Page<BundleSubmission> pageOfBundleSubmissions = bundleSubmissionService.getPageOfBundleSubmissions(pageIndex, PAGE_SIZE, orderBy, orderDir, null, problem.getJid(), null);

        LazyHtml content = new LazyHtml(listSubmissionsView.render(pageOfBundleSubmissions, problemId, pageIndex, orderBy, orderDir));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.bundle.submission.list"), routes.BundleProblemSubmissionController.viewSubmissions(problemId)));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Submissions");

        ControllerUtils.getInstance().addActivityLog("List submissions of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    public Result viewSubmission(long problemId, long submissionId) throws ProblemNotFoundException, BundleSubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            return notFound();
        }

        BundleSubmission bundleSubmission = bundleSubmissionService.findBundleSubmissionById(submissionId);
        BundleAnswer bundleAnswer;
        try {
            bundleAnswer = bundleSubmissionService.createBundleAnswerFromPastSubmission(bundleSubmissionFileSystemProvider, null, bundleSubmission.getJid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LazyHtml content = new LazyHtml(bundleSubmissionView.render(bundleSubmission, new Gson().fromJson(bundleSubmission.getLatestDetails(), new TypeToken<Map<String, BundleDetailResult>>() { }.getType()), bundleAnswer, JidCacheServiceImpl.getInstance().getDisplayName(bundleSubmission.getAuthorJid()), null, problem.getName(), null));

        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.submission.view"), routes.ProgrammingProblemSubmissionController.viewSubmission(problemId, submissionId)));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View Submission");

        ControllerUtils.getInstance().addActivityLog("View submission " + submissionId + " of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional
    public Result regradeSubmission(long problemId, long submissionId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException, BundleSubmissionNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            return notFound();
        }

        BundleSubmission bundleSubmission = bundleSubmissionService.findBundleSubmissionById(submissionId);
        BundleAnswer bundleAnswer;
        try {
            bundleAnswer = bundleSubmissionService.createBundleAnswerFromPastSubmission(bundleSubmissionFileSystemProvider, null, bundleSubmission.getJid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bundleSubmissionService.regrade(bundleSubmission.getJid(), bundleAnswer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        ControllerUtils.getInstance().addActivityLog("Regrade submission " + submissionId + " of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
    }

    @Transactional
    public Result regradeSubmissions(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!BundleProblemControllerUtils.isAllowedToSubmit(problemService, problem)) {
            return notFound();
        }

        ListTableSelectionForm data = Form.form(ListTableSelectionForm.class).bindFromRequest().get();

        List<BundleSubmission> submissions;

        if (data.selectAll) {
            submissions = bundleSubmissionService.getBundleSubmissionsByFilters(orderBy, orderDir, null, problem.getJid(), null);
        } else if (data.selectJids != null) {
            submissions = bundleSubmissionService.getBundleSubmissionsByJids(data.selectJids);
        } else {
            return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
        }

        for (BundleSubmission submission : submissions) {
            BundleAnswer bundleAnswer;
            try {
                bundleAnswer = bundleSubmissionService.createBundleAnswerFromPastSubmission(bundleSubmissionFileSystemProvider, null, submission.getJid());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            bundleSubmissionService.regrade(submission.getJid(), bundleAnswer, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
        }

        ControllerUtils.getInstance().addActivityLog("Regrade submissions of bundle problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleProblemSubmissionController.listSubmissions(problemId, pageIndex, orderBy, orderDir));
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
