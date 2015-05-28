package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemService;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemStatementUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class BundleProblemController extends BaseController {

    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;

    public BundleProblemController(ProblemService problemService, BundleProblemService bundleProblemService) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
    }

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
