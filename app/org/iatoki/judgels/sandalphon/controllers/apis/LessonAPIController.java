package org.iatoki.judgels.sandalphon.controllers.apis;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.lessonStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public final class LessonAPIController extends AbstractJudgelsAPIController {

    private final ClientService clientService;
    private final LessonService lessonService;

    @Inject
    public LessonAPIController(ClientService clientService, LessonService lessonService) {
        this.clientService = clientService;
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Result verifyLesson() {
        UsernamePasswordCredentials credentials = JudgelsPlayUtils.parseBasicAuthFromRequest(request());

        if (credentials == null) {
            response().setHeader("WWW-Authenticate", "Basic realm=\"" + request().host() + "\"");
            return unauthorized();
        }

        String clientJid = credentials.getUserName();
        String clientSecret = credentials.getPassword();
        if (!clientService.clientExistsByJid(clientJid)) {
            return notFound();
        }

        Client client = clientService.findClientByJid(clientJid);
        if (!client.getSecret().equals(clientSecret)) {
            return forbidden();
        }

        DynamicForm dForm = DynamicForm.form().bindFromRequest();

        String lessonJid = dForm.get("lessonJid");
        if (!lessonService.lessonExistsByJid(lessonJid)) {
            return notFound();
        }

        return ok(lessonService.findLessonByJid(lessonJid).getName());
    }

    @Transactional(readOnly = true)
    public Result viewLessonStatementTOTP() {
        response().setHeader("Access-Control-Allow-Origin", "*");

        DynamicForm dForm = DynamicForm.form().bindFromRequest();

        String clientJid = dForm.get("clientJid");
        String lessonJid = dForm.get("lessonJid");
        int tOTP = 0;
        if (dForm.get("TOTP") != null) {
            tOTP = Integer.parseInt(dForm.get("TOTP"));
        }
        String lang = dForm.get("lang");
        String switchLanguageUri = dForm.get("switchLanguageUri");

        if ((!clientService.clientExistsByJid(clientJid)) && (!lessonService.lessonExistsByJid(lessonJid)) && (!clientService.isClientAuthorizedForLesson(lessonJid, clientJid))) {
            return notFound();
        }

        Lesson lesson = lessonService.findLessonByJid(lessonJid);
        ClientLesson clientLesson = clientService.findClientLessonByClientJidAndLessonJid(clientJid, lessonJid);

        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        if (!googleAuthenticator.authorize(new Base32().encodeAsString(clientLesson.getSecret().getBytes()), tOTP)) {
            return forbidden();
        }

        LazyHtml content;
        try {
            Map<String, StatementLanguageStatus> availableStatementLanguages = lessonService.getAvailableLanguages(null, lesson.getJid());

            if (!availableStatementLanguages.containsKey(lang) || availableStatementLanguages.get(lang) == StatementLanguageStatus.DISABLED) {
                lang = lessonService.getDefaultLanguage(null, lessonJid);
            }

            String language = lang;
            String statement = lessonService.getStatement(null, lessonJid, lang);

            Set<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet());

            Html html = lessonStatementView.render(lesson.getName(), statement);
            content = new LazyHtml(html);
            if (switchLanguageUri != null) {
                content.appendLayout(c -> statementLanguageSelectionLayout.render(switchLanguageUri, allowedStatementLanguages, language, c));
            }
        } catch (IOException e) {
            return notFound();
        }

        return ok(content.render());
    }
}
