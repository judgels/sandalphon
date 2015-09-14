package org.iatoki.judgels.sandalphon.controllers.api.client.v1;

import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public final class ClientProblemStatementAPIControllerV1 extends AbstractJudgelsAPIController {

    private final ProblemService problemService;

    @Inject
    public ClientProblemStatementAPIControllerV1(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result renderMediaByJid(String problemJid, String mediaFilename) {
        String mediaUrl = problemService.getStatementMediaFileURL(null, problemJid, mediaFilename);
        return okAsImage(mediaUrl);
    }
}
