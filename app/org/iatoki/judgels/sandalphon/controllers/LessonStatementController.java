package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonService;
import org.iatoki.judgels.sandalphon.LessonStatementUtils;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.commons.WorldLanguageRegistry;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.lessonStatementView;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.listStatementLanguagesView;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.lesson.statement.updateStatementView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public class LessonStatementController extends BaseController {
    private final LessonService lessonService;

    public LessonStatementController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    public Result viewStatement(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        try {
            LessonControllerUtils.establishStatementLanguage(lessonService, lesson);
        } catch (IOException e) {
            return notFound();
        }

        if (LessonControllerUtils.isAllowedToViewStatement(lessonService, lesson)) {
            String statement;
            try {
                statement = lessonService.getStatement(IdentityUtils.getUserJid(), lesson.getJid(), LessonControllerUtils.getCurrentStatementLanguage());
            } catch (IOException e) {
                statement = LessonStatementUtils.getDefaultStatement(LessonControllerUtils.getCurrentStatementLanguage());
            }

            try {
                LazyHtml content = new LazyHtml(lessonStatementView.render(lesson.getName(), statement));

                Set<String> allowedLanguages = LessonControllerUtils.getAllowedLanguagesToView(lessonService, lesson);

                LessonControllerUtils.appendStatementLanguageSelectionLayout(content, LessonControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.LessonStatementController.viewStatementSwitchLanguage(lesson.getId()));

                LessonStatementControllerUtils.appendSubtabsLayout(content, lessonService, lesson);
                LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
                LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
                LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
                ControllerUtils.getInstance().appendSidebarLayout(content);
                LessonStatementControllerUtils.appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.statement.view"), routes.LessonStatementController.viewStatement(lessonId)));
                ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Statement");

                ControllerUtils.getInstance().addActivityLog("View statement of programming lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return ControllerUtils.getInstance().lazyOk(content);
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    public Result viewStatementSwitchLanguage(long lessonId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        LessonControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch view statement to " + languageCode + " of lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonStatementController.viewStatement(lessonId));
    }

    public Result updateStatementSwitchLanguage(long lessonId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        LessonControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch update statement to " + languageCode + " of lesson " + lessonId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.LessonStatementController.updateStatement(lessonId));
    }

    @AddCSRFToken
    public Result updateStatement(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        try {
            LessonControllerUtils.establishStatementLanguage(lessonService, lesson);
        } catch (IOException e) {
            return notFound();
        }

        if (LessonControllerUtils.isAllowedToUpdateStatementInLanguage(lessonService, lesson)) {
            String statement;
            try {
                statement = lessonService.getStatement(IdentityUtils.getUserJid(), lesson.getJid(), LessonControllerUtils.getCurrentStatementLanguage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
            form = form.bind(ImmutableMap.of("statement", statement));

            try {
                Set<String> allowedLanguages = LessonControllerUtils.getAllowedLanguagesToUpdate(lessonService, lesson);

                ControllerUtils.getInstance().addActivityLog("Try to update statement of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return showUpdateStatement(form, lesson, allowedLanguages);
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postUpdateStatement(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);
        try {
            LessonControllerUtils.establishStatementLanguage(lessonService, lesson);
        } catch (IOException e) {
            return notFound();
        }

        if (LessonControllerUtils.isAllowedToUpdateStatementInLanguage(lessonService, lesson)) {
            Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
            if (form.hasErrors() || form.hasGlobalErrors()) {
                try {
                    Set<String> allowedLanguages = LessonControllerUtils.getAllowedLanguagesToUpdate(lessonService, lesson);
                    return showUpdateStatement(form, lesson, allowedLanguages);
                } catch (IOException e) {
                    return notFound();
                }
            } else {
                lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

                try {
                    lessonService.updateStatement(IdentityUtils.getUserJid(), lessonId, LessonControllerUtils.getCurrentStatementLanguage(), form.get().statement);

                    ControllerUtils.getInstance().addActivityLog("Update statement of lesson " + lesson.getName() + ".");

                    return redirect(routes.LessonStatementController.updateStatement(lesson.getId()));
                } catch (IOException e) {
                    try {
                        form.reject("lesson.statement.error.cantUpload");
                        Set<String> allowedLanguages = LessonControllerUtils.getAllowedLanguagesToUpdate(lessonService, lesson);
                        return showUpdateStatement(form, lesson, allowedLanguages);
                    } catch (IOException e2) {
                        return notFound();
                    }
                }
            }
        } else {
            return notFound();
        }
    }


    @AddCSRFToken
    public Result listStatementMediaFiles(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        Form<UploadFileForm> form = Form.form(UploadFileForm.class);
        
        boolean isAllowedToUploadMediaFiles = LessonControllerUtils.isAllowedToUploadStatementResources(lessonService, lesson);
        
        List<FileInfo> mediaFiles = lessonService.getStatementMediaFiles(IdentityUtils.getUserJid(), lesson.getJid());

        ControllerUtils.getInstance().addActivityLog("List statement media files of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showListStatementMediaFiles(form, lesson, mediaFiles, isAllowedToUploadMediaFiles);
    }

    @RequireCSRFCheck
    public Result postUploadStatementMediaFiles(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToUploadStatementResources(lessonService, lesson)) {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart file;

            file = body.getFile("file");
            if (file != null) {
                File mediaFile = file.getFile();
                lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

                try {
                    lessonService.uploadStatementMediaFile(IdentityUtils.getUserJid(), lesson.getId(), mediaFile, file.getFilename());

                    ControllerUtils.getInstance().addActivityLog("Upload statement media file of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                    return redirect(routes.LessonStatementController.listStatementMediaFiles(lesson.getId()));
                } catch (IOException e) {
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    form.reject("lesson.statement.error.cantUploadMedia");
                    boolean isAllowedToUploadMediaFiles = LessonControllerUtils.isAllowedToUploadStatementResources(lessonService, lesson);
                    List<FileInfo> mediaFiles = lessonService.getStatementMediaFiles(IdentityUtils.getUserJid(), lesson.getJid());

                    return showListStatementMediaFiles(form, lesson, mediaFiles, isAllowedToUploadMediaFiles);
                }
            }

            file = body.getFile("fileZipped");
            if (file != null) {
                File mediaFile = file.getFile();
                lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

                try {
                    lessonService.uploadStatementMediaFileZipped(IdentityUtils.getUserJid(), lesson.getId(), mediaFile);

                    ControllerUtils.getInstance().addActivityLog("Upload statement zipped media files of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                    return redirect(routes.LessonStatementController.listStatementMediaFiles(lesson.getId()));
                } catch (IOException e) {
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    form.reject("lesson.statement.error.cantUploadMediaZipped");
                    boolean isAllowedToUploadMediaFiles = LessonControllerUtils.isAllowedToUploadStatementResources(lessonService, lesson);
                    List<FileInfo> mediaFiles = lessonService.getStatementMediaFiles(IdentityUtils.getUserJid(), lesson.getJid());

                    return showListStatementMediaFiles(form, lesson, mediaFiles, isAllowedToUploadMediaFiles);
                }
            }

            return redirect(routes.LessonStatementController.listStatementMediaFiles(lesson.getId()));
        } else {
            return notFound();
        }
    }

    public Result downloadStatementMediaFile(long id, String filename) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(id);
        String mediaURL = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), filename);

        ControllerUtils.getInstance().addActivityLog("Download media file " + filename + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File mediaFile = new File(mediaURL);
            return LessonControllerUtils.downloadFile(mediaFile);
        }
    }

    public Result listStatementLanguages(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            try {
                Map<String, StatementLanguageStatus> availableLanguages = lessonService.getAvailableLanguages(IdentityUtils.getUserJid(), lesson.getJid());
                String defaultLanguage = lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid());

                LazyHtml content = new LazyHtml(listStatementLanguagesView.render(availableLanguages, defaultLanguage, lesson.getId()));
                LessonStatementControllerUtils.appendSubtabsLayout(content, lessonService, lesson);
                LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
                LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
                LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
                ControllerUtils.getInstance().appendSidebarLayout(content);
                LessonStatementControllerUtils.appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.statement.language.list"), routes.LessonStatementController.listStatementLanguages(lesson.getId())));
                ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Statement Languages");

                ControllerUtils.getInstance().addActivityLog("List statement languages of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return ControllerUtils.getInstance().lazyOk(content);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return notFound();
        }
    }


    public Result postAddStatementLanguage(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

            try {
                String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
                if (WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                    lessonService.addLanguage(IdentityUtils.getUserJid(), lesson.getJid(), languageCode);

                    ControllerUtils.getInstance().addActivityLog("Add statement language " + languageCode + " of lesson " + lesson.getName() + ".");

                    return redirect(routes.LessonStatementController.listStatementLanguages(lesson.getId()));
                } else {
                    // TODO should use form so it can be rejected
                    throw new IllegalStateException("Languages is not from list.");
                }
            } catch (IOException e) {
                // TODO should use form so it can be rejected
                throw new IllegalStateException(e);
            }
        } else {
            return notFound();
        }
    }

    public Result enableStatementLanguage(long lessonId, String languageCode) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

            try {
                // TODO should check if language has been enabled
                if (WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                    lessonService.enableLanguage(IdentityUtils.getUserJid(), lesson.getJid(), languageCode);

                    ControllerUtils.getInstance().addActivityLog("Enable statement language " + languageCode + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                    return redirect(routes.LessonStatementController.listStatementLanguages(lesson.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Statement language probably hasn't been added.", e);
            }
        } else {
            return notFound();
        }
    }


    public Result disableStatementLanguage(long lessonId, String languageCode) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

            try {
                // TODO should check if language has been enabled
                if (WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                    lessonService.disableLanguage(IdentityUtils.getUserJid(), lesson.getJid(), languageCode);

                    if (LessonControllerUtils.getCurrentStatementLanguage().equals(languageCode)) {
                        LessonControllerUtils.setCurrentStatementLanguage(lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid()));
                    }

                    ControllerUtils.getInstance().addActivityLog("Disable statement language " + languageCode + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                    return redirect(routes.LessonStatementController.listStatementLanguages(lesson.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Statement language probably hasn't been added.", e);
            }
        } else {
            return notFound();
        }
    }

    public Result makeDefaultStatementLanguage(long lessonId, String languageCode) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            lessonService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), lesson.getJid());

            try {
                // TODO should check if language has been enabled
                if (WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                    lessonService.makeDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid(), languageCode);

                    ControllerUtils.getInstance().addActivityLog("Make statement language " + languageCode + " default of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                    return redirect(routes.LessonStatementController.listStatementLanguages(lesson.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Statement language probably hasn't been added.", e);
            }
        } else {
            return notFound();
        }
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, Lesson lesson, Set<String> allowedLanguages) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, lesson.getId()));
        LessonControllerUtils.appendStatementLanguageSelectionLayout(content, LessonControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.LessonStatementController.updateStatementSwitchLanguage(lesson.getId()));
        LessonStatementControllerUtils.appendSubtabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        LessonStatementControllerUtils.appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.statement.update"), routes.LessonStatementController.updateStatement(lesson.getId())));

        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListStatementMediaFiles(Form<UploadFileForm> form, Lesson lesson, List<FileInfo> mediaFiles, boolean isAllowedToUploadMediaFiles) {
        LazyHtml content = new LazyHtml(listStatementMediaFilesView.render(form, lesson.getId(), mediaFiles, isAllowedToUploadMediaFiles));
        LessonStatementControllerUtils.appendSubtabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        LessonStatementControllerUtils.appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.statement.media.list"), routes.LessonStatementController.listStatementMediaFiles(lesson.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Statement - List Media");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
