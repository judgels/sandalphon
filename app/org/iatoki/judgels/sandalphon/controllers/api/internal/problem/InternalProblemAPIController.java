package org.iatoki.judgels.sandalphon.controllers.api.internal.problem;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Singleton
@Named
public final class InternalProblemAPIController extends AbstractJudgelsAPIController {

    private final ProblemService problemService;

    @Inject
    public InternalProblemAPIController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long problemId, String mediaFilename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        String mediaUrl = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), mediaFilename);

        try {
            new URL(mediaUrl);
            return temporaryRedirect(mediaUrl);
        } catch (MalformedURLException e) {
            File imageFile = new File(mediaUrl);
            if (!imageFile.exists()) {
                return notFound();
            }

            return okAsImage(imageFile);
        }
    }
}
