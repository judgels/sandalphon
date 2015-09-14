package org.iatoki.judgels.sandalphon.controllers.api.internal;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.SandalphonControllerUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.LessonService;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class InternalLessonStatementAPIController extends AbstractJudgelsAPIController {

    private final LessonService lessonService;

    @Inject
    public InternalLessonStatementAPIController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long lessonId, String mediaFilename) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        String mediaUrl = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), mediaFilename);

        return okAsImage(mediaUrl);
    }

    @Transactional(readOnly = true)
    public Result downloadStatementMediaFile(long id, String filename) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(id);
        String mediaUrl = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), filename);

        SandalphonControllerUtils.getInstance().addActivityLog("Download media file " + filename + " of lesson " + lesson.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return okAsDownload(mediaUrl);
    }
}
