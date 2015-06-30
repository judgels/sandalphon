package org.iatoki.judgels.sandalphon.controllers.apis;

import org.iatoki.judgels.sandalphon.services.GraderService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

@Singleton
@Named
public final class ProgrammingProblemAPIController extends Controller {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;
    private final GraderService graderService;

    @Inject
    public ProgrammingProblemAPIController(ProblemService problemService, ProgrammingProblemService programmingProblemService, GraderService graderService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
        this.graderService = graderService;
    }

    @Transactional(readOnly = true)
    public Result downloadGradingFiles() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

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
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

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
