package org.iatoki.judgels.sandalphon.controllers.api.client.v1;

import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.LessonV1;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.LessonService;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Named
public final class ClientLessonAPIControllerV1 extends AbstractJudgelsAPIController {

    private final ClientService clientService;
    private final LessonService lessonService;

    @Inject
    public ClientLessonAPIControllerV1(ClientService clientService, LessonService lessonService) {
        this.clientService = clientService;
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result findLessonByJid(String lessonJid) {
        authenticateAsJudgelsAppClient(clientService);

        if (!lessonService.lessonExistsByJid(lessonJid)) {
            throw new JudgelsAPINotFoundException();
        }

        try {
            Lesson lesson = lessonService.findLessonByJid(lessonJid);

            LessonV1 responseBody = new LessonV1();

            responseBody.jid = lesson.getJid();
            responseBody.slug = lesson.getSlug();
            responseBody.defaultLanguage = lessonService.getDefaultLanguage(null, lessonJid);
            responseBody.titlesByLanguage = lessonService.getTitlesByLanguage(null, lessonJid);

            return okAsJson(responseBody);
        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }
}
