package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.ProblemStatementUtils;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleProblemStatementUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class BundleProblemController extends AbstractJudgelsController {

    private final BundleProblemService bundleProblemService;
    private final ProblemService problemService;

    @Inject
    public BundleProblemController(BundleProblemService bundleProblemService, ProblemService problemService) {
        this.bundleProblemService = bundleProblemService;
        this.problemService = problemService;
    }

    @Transactional
    public Result createBundleProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        String slug = ProblemControllerUtils.getJustCreatedProblemSlug();
        String additionalNote = ProblemControllerUtils.getJustCreatedProblemAdditionalNote();
        String languageCode = ProblemControllerUtils.getJustCreatedProblemInitLanguageCode();

        Problem problem;
        try {
            problem = problemService.createProblem(ProblemType.BUNDLE, slug, additionalNote, languageCode);
            problemService.updateStatement(null, problem.getId(), languageCode, new ProblemStatement(ProblemStatementUtils.getDefaultTitle(languageCode), BundleProblemStatementUtils.getDefaultStatement(languageCode)));
            bundleProblemService.initBundleProblem(problem.getJid());
        } catch (IOException e) {
            e.printStackTrace();
            return internalServerError();
        }

        problemService.initRepository(IdentityUtils.getUserJid(), problem.getJid());

        ProblemControllerUtils.setCurrentStatementLanguage(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
        ProblemControllerUtils.removeJustCreatedProblem();

        SandalphonControllerUtils.getInstance().addActivityLog("Create bundle problem <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemController.enterProblem(problem.getId()));
    }

    public Result jumpToItems(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to bundle problem items " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleItemController.viewItems(problemId));
    }

    public Result jumpToSubmissions(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to bundle problem submissions " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleProblemSubmissionController.viewSubmissions(problemId));
    }
}
