package org.iatoki.judgels.sandalphon.controllers.api.internal;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemControllerUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public final class InternalProgrammingProblemGradingAPIController extends AbstractJudgelsAPIController {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public InternalProgrammingProblemGradingAPIController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Transactional(readOnly = true)
    public Result downloadGradingTestDataFile(long problemId, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            throw new JudgelsAPINotFoundException();
        }

        String testDataUrl = programmingProblemService.getGradingTestDataFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        return okAsDownload(testDataUrl);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Transactional(readOnly = true)
    public Result downloadGradingHelperFile(long problemId, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            throw new JudgelsAPINotFoundException();
        }

        String helper = programmingProblemService.getGradingHelperFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        return okAsDownload(helper);
    }
}
