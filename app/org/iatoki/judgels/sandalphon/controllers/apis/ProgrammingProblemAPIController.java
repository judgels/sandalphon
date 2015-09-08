package org.iatoki.judgels.sandalphon.controllers.apis;

import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.services.GraderService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

@Singleton
@Named
public final class ProgrammingProblemAPIController extends AbstractJudgelsAPIController {

    private final GraderService graderService;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ProgrammingProblemAPIController(GraderService graderService, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.graderService = graderService;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    public Result downloadGradingFiles() {
        DynamicForm dForm = DynamicForm.form().bindFromRequest();

        String graderJid = dForm.get("graderJid");
        String graderSecret = dForm.get("graderSecret");
        String problemJid = dForm.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        try {
            ByteArrayOutputStream os = programmingProblemService.getZippedGradingFilesStream(problemJid);
            response().setContentType("application/x-download");
            response().setHeader("Content-disposition", "attachment; filename=" + problemJid + ".zip");
            return ok(os.toByteArray()).as("application/zip");
        } catch (IOException e) {
            return internalServerError();
        }
    }

    @Transactional(readOnly = true)
    public Result getGradingLastUpdateTime() {
        DynamicForm dForm = DynamicForm.form().bindFromRequest();

        String graderJid = dForm.get("graderJid");
        String graderSecret = dForm.get("graderSecret");
        String problemJid = dForm.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        try {
            Date gradingLastUpdateTime = programmingProblemService.getGradingLastUpdateTime(null, problemJid);

            return ok("" + gradingLastUpdateTime.getTime());
        } catch (IOException e) {
            return internalServerError();
        }
    }

}
