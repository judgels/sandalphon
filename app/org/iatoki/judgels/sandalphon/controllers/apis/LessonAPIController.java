package org.iatoki.judgels.sandalphon.controllers.apis;

import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

@Transactional
public final class LessonAPIController extends Controller {
    private final LessonService lessonService;
    private final ClientService clientService;

    public LessonAPIController(LessonService lessonService, ClientService clientService) {
        this.lessonService = lessonService;
        this.clientService = clientService;
    }

    public Result renderMediaById(long lessonId, String filename) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        String mediaURL = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), filename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File file = new File(mediaURL);

            if (!file.exists()) {
                return notFound();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            response().setHeader("Cache-Control", "no-transform,public,max-age=300,s-maxage=900");
            response().setHeader("Last-Modified", sdf.format(new Date(file.lastModified())));

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                FileUtils.copyFile(file, baos);
                String mimeType = URLConnection.guessContentTypeFromName(filename);
                return ok(baos.toByteArray()).as(mimeType);
            } catch (IOException e2) {
                return internalServerError();
            }
        }
    }

    public Result renderMediaByJid(String lessonJid, String filename) {
        String mediaURL = lessonService.getStatementMediaFileURL(null, lessonJid, filename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File file = new File(mediaURL);

            if (!file.exists()) {
                return notFound();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            response().setHeader("Cache-Control", "no-transform,public,max-age=300,s-maxage=900");
            response().setHeader("Last-Modified", sdf.format(new Date(file.lastModified())));

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                FileUtils.copyFile(file, baos);
                String mimeType = URLConnection.guessContentTypeFromName(filename);
                return ok(baos.toByteArray()).as(mimeType);
            } catch (IOException e2) {
                return internalServerError();
            }
        }
    }

    public Result verifyLesson() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String clientJid = form.get("clientJid");
        String clientSecret = form.get("clientSecret");
        if (clientService.clientExistsByClientJid(clientJid)) {
            Client client = clientService.findClientByJid(clientJid);
            if (client.getSecret().equals(clientSecret)) {
                String lessonJid = form.get("lessonJid");
                if (lessonService.lessonExistsByJid(lessonJid)) {
                    return ok(lessonService.findLessonByJid(lessonJid).getName());
                } else {
                    return notFound();
                }
            } else {
                return forbidden();
            }
        } else {
            return notFound();
        }
    }
}
