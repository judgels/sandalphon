package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemStatementUtils;
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

    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;

    @Inject
    public BundleProblemController(ProblemService problemService, BundleProblemService bundleProblemService) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
    }

    @Transactional
    public Result createBundleProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        try {
            Problem problem = problemService.createProblem(ProblemType.BUNDLE, ProblemControllerUtils.getJustCreatedProblemName(), ProblemControllerUtils.getJustCreatedProblemAdditionalNote(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            problemService.updateStatement(null, problem.getId(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode(), BundleProblemStatementUtils.getDefaultStatement(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode()));
            bundleProblemService.initBundleProblem(problem.getJid());
            problemService.initRepository(IdentityUtils.getUserJid(), problem.getJid());

            ProblemControllerUtils.setCurrentStatementLanguage(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            ProblemControllerUtils.removeJustCreatedProblem();

            ControllerUtils.getInstance().addActivityLog("Create bundle problem <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProblemController.enterProblem(problem.getId()));
        } catch (IOException e) {
            e.printStackTrace();
            return internalServerError();
        }
    }

    public Result jumpToItems(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to bundle problem items " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleItemController.viewItems(problemId));
    }

    public Result jumpToSubmissions(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to bundle problem submissions " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.BundleProblemSubmissionController.viewSubmissions(problemId));
    }

}
