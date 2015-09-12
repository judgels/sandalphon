package org.iatoki.judgels.sandalphon.controllers.api.client.v1.problem.programming;

import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.ProgrammingProblemInfoV1;
import org.iatoki.judgels.sandalphon.services.GraderService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Singleton
@Named
public final class ClientProgrammingProblemAPIControllerV1 extends AbstractJudgelsAPIController {

    private final GraderService graderService;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ClientProgrammingProblemAPIControllerV1(GraderService graderService, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.graderService = graderService;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    public Result downloadGradingFiles(String problemJid) {
        authenticateAsJudgelsAppClient(graderService);

        try {
            ByteArrayOutputStream os = programmingProblemService.getZippedGradingFilesStream(problemJid);
            response().setContentType("application/x-download");
            response().setHeader("Content-disposition", "attachment; filename=" + problemJid + ".zip");
            return ok(os.toByteArray()).as("application/zip");
        } catch (IOException e) {
            e.printStackTrace();
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }


    @Transactional(readOnly = true)
    public Result getProgrammingProblemInfo(String problemJid) {
        authenticateAsJudgelsAppClient(graderService);

        if (!problemService.problemExistsByJid(problemJid)) {
            throw new JudgelsAPINotFoundException();
        }

        try {
            ProgrammingProblemInfoV1 responseBody = new ProgrammingProblemInfoV1();

            responseBody.gradingEngine = programmingProblemService.getGradingEngine(null, problemJid);
            responseBody.gradingLastUpdateTime = programmingProblemService.getGradingLastUpdateTime(null, problemJid).getTime();

            return okAsJson(responseBody);
        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }
}
