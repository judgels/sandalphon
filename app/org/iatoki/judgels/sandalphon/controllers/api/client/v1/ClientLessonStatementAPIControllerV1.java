package org.iatoki.judgels.sandalphon.controllers.api.client.v1;

import org.iatoki.judgels.play.apis.JudgelsAPIForbiddenException;
import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonStatement;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.LessonStatementRenderRequestV1;
import org.iatoki.judgels.sandalphon.controllers.api.util.TOTPUtils;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.lessonStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public final class ClientLessonStatementAPIControllerV1 extends AbstractJudgelsAPIController {

    private final ClientService clientService;
    private final LessonService lessonService;

    @Inject
    public ClientLessonStatementAPIControllerV1(ClientService clientService, LessonService lessonService) {
        this.clientService = clientService;
        this.lessonService = lessonService;
    }

    public Result renderMediaByJid(String lessonJid, String mediaFilename) {
        String mediaUrl = lessonService.getStatementMediaFileURL(null, lessonJid, mediaFilename);
        return okAsImage(mediaUrl);
    }

    @Transactional(readOnly = true)
    public Result renderStatement(String lessonJid) {
        LessonStatementRenderRequestV1 requestBody = parseRequestBodyAsUrlFormEncoded(LessonStatementRenderRequestV1.class);

        if (!lessonService.lessonExistsByJid(lessonJid)) {
            throw new JudgelsAPINotFoundException();
        }
        if (!clientService.clientExistsByJid(requestBody.clientJid)) {
            throw new JudgelsAPIForbiddenException("Client not exists");
        }
        if (!clientService.isClientAuthorizedForLesson(requestBody.lessonJid, requestBody.clientJid)) {
            throw new JudgelsAPIForbiddenException("Client not authorized to view lesson");
        }

        Lesson lesson = lessonService.findLessonByJid(lessonJid);
        ClientLesson clientLesson = clientService.findClientLessonByClientJidAndLessonJid(requestBody.clientJid, lessonJid);

        if (!TOTPUtils.match(clientLesson.getSecret(), requestBody.totpCode)) {
            throw new JudgelsAPIForbiddenException("TOTP code mismatch");
        }

        try {
            Map<String, StatementLanguageStatus> availableStatementLanguages = lessonService.getAvailableLanguages(null, lesson.getJid());

            String statementLanguage = requestBody.statementLanguage;
            if (!availableStatementLanguages.containsKey(statementLanguage) || availableStatementLanguages.get(statementLanguage) == StatementLanguageStatus.DISABLED) {
                statementLanguage = lessonService.getDefaultLanguage(null, lessonJid);
            }

            LessonStatement statement = lessonService.getStatement(null, lessonJid, statementLanguage);

            Set<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet());

            Html statementHtml = lessonStatementView.render(statement);
            if (requestBody.switchStatementLanguageUrl != null) {
                statementHtml = statementLanguageSelectionLayout.render(requestBody.switchStatementLanguageUrl, allowedStatementLanguages, statementLanguage, statementHtml);
            }

            return ok(statementHtml);
        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }
}
