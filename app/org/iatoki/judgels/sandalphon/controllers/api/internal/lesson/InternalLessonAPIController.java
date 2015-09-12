package org.iatoki.judgels.sandalphon.controllers.api.internal.lesson;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.services.LessonService;
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
public final class InternalLessonAPIController extends AbstractJudgelsAPIController {

    private final LessonService lessonService;

    @Inject
    public InternalLessonAPIController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long lessonId, String mediaFilename) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        String mediaUrl = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), mediaFilename);

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
