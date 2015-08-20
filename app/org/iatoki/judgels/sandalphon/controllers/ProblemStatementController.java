package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.FileInfo;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.WorldLanguageRegistry;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.ProgrammingProblemStatementUtils;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.listStatementLanguagesView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.updateStatementView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public class ProblemStatementController extends AbstractJudgelsController {

    private final ProblemService problemService;

    @Inject
    public ProblemStatementController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result viewStatement(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (problem.getType().equals(ProblemType.PROGRAMMING)) {
            return redirect(routes.ProgrammingProblemStatementController.viewStatement(problem.getId()));
        } else if (problem.getType().equals(ProblemType.BUNDLE)) {
            return redirect(routes.BundleProblemStatementController.viewStatement(problem.getId()));
        }

        return badRequest();
    }

    public Result viewStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch view statement to " + languageCode + " of problem " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.viewStatement(problemId));
    }

    public Result updateStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch update statement to " + languageCode + " of problem " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.updateStatement(problemId));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateStatement(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        if (!ProblemControllerUtils.isAllowedToUpdateStatementInLanguage(problemService, problem)) {
            return notFound();
        }

        String statement;
        try {
            statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());
        } catch (IOException e) {
            if (ProblemType.PROGRAMMING.equals(problem.getType())) {
                statement = ProgrammingProblemStatementUtils.getDefaultStatement(ProblemControllerUtils.getCurrentStatementLanguage());
            } else {
                throw new IllegalStateException("Problem besides programming has not been defined");
            }
        }

        Form<UpdateStatementForm> updateStatementForm = Form.form(UpdateStatementForm.class);
        updateStatementForm = updateStatementForm.bind(ImmutableMap.of("statement", statement));

        Set<String> allowedLanguages;
        try {
            allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        ControllerUtils.getInstance().addActivityLog("Try to update statement of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdateStatement(updateStatementForm, problem, allowedLanguages);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateStatement(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        if (!ProblemControllerUtils.isAllowedToUpdateStatementInLanguage(problemService, problem)) {
            return notFound();
        }

        Form<UpdateStatementForm> updateStatementForm = Form.form(UpdateStatementForm.class).bindFromRequest();
        if (formHasErrors(updateStatementForm)) {
            try {
                Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
                return showUpdateStatement(updateStatementForm, problem, allowedLanguages);
            } catch (IOException e) {
                return notFound();
            }
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        try {
            problemService.updateStatement(IdentityUtils.getUserJid(), problemId, ProblemControllerUtils.getCurrentStatementLanguage(), updateStatementForm.get().statement);
        } catch (IOException e) {
            try {
                updateStatementForm.reject("problem.statement.error.cantUpload");
                Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
                return showUpdateStatement(updateStatementForm, problem, allowedLanguages);
            } catch (IOException e2) {
                return notFound();
            }
        }

        ControllerUtils.getInstance().addActivityLog("Update statement of problem " + problem.getName() + ".");

        return redirect(routes.ProblemStatementController.updateStatement(problem.getId()));
    }


    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result listStatementMediaFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        Form<UploadFileForm> uploadFileForm = Form.form(UploadFileForm.class);
        boolean isAllowedToUploadMediaFiles = ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem);
        List<FileInfo> mediaFiles = problemService.getStatementMediaFiles(IdentityUtils.getUserJid(), problem.getJid());

        ControllerUtils.getInstance().addActivityLog("List statement media files of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showListStatementMediaFiles(uploadFileForm, problem, mediaFiles, isAllowedToUploadMediaFiles);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUploadStatementMediaFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem)) {
            return notFound();
        }

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            try {
                problemService.uploadStatementMediaFile(IdentityUtils.getUserJid(), problem.getId(), mediaFile, file.getFilename());
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                form.reject("problem.statement.error.cantUploadMedia");
                boolean isAllowedToUploadMediaFiles = ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem);
                List<FileInfo> mediaFiles = problemService.getStatementMediaFiles(IdentityUtils.getUserJid(), problem.getJid());

                return showListStatementMediaFiles(form, problem, mediaFiles, isAllowedToUploadMediaFiles);
            }

            ControllerUtils.getInstance().addActivityLog("Upload statement media file of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            try {
                problemService.uploadStatementMediaFileZipped(IdentityUtils.getUserJid(), problem.getId(), mediaFile);
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                form.reject("problem.statement.error.cantUploadMediaZipped");
                boolean isAllowedToUploadMediaFiles = ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem);
                List<FileInfo> mediaFiles = problemService.getStatementMediaFiles(IdentityUtils.getUserJid(), problem.getJid());

                return showListStatementMediaFiles(form, problem, mediaFiles, isAllowedToUploadMediaFiles);
            }

            ControllerUtils.getInstance().addActivityLog("Upload statement zipped media files of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
        }

        return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result downloadStatementMediaFile(long id, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(id);
        String mediaURL = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        ControllerUtils.getInstance().addActivityLog("Download media file " + filename + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File mediaFile = new File(mediaURL);
            return ProblemControllerUtils.downloadFile(mediaFile);
        }
    }

    @Transactional(readOnly = true)
    public Result listStatementLanguages(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            return notFound();
        }

        Map<String, StatementLanguageStatus> availableLanguages;
        String defaultLanguage;
        try {
            availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());
            defaultLanguage = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        LazyHtml content = new LazyHtml(listStatementLanguagesView.render(availableLanguages, defaultLanguage, problem.getId()));
        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.language.list"), routes.ProblemStatementController.listStatementLanguages(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Statement Languages");

        ControllerUtils.getInstance().addActivityLog("List statement languages of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional
    public Result postAddStatementLanguage(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        String languageCode;
        try {
            languageCode = DynamicForm.form().bindFromRequest().get("langCode");
            if (!WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                // TODO should use form so it can be rejected
                throw new IllegalStateException("Languages is not from list.");
            }

            problemService.addLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);
        } catch (IOException e) {
            // TODO should use form so it can be rejected
            throw new IllegalStateException(e);
        }

        ControllerUtils.getInstance().addActivityLog("Add statement language " + languageCode + " of problem " + problem.getName() + ".");

        return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result enableStatementLanguage(long problemId, String languageCode) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        try {
            // TODO should check if language has been enabled
            if (!WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                return notFound();
            }

            problemService.enableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);
        } catch (IOException e) {
            throw new IllegalStateException("Statement language probably hasn't been added.", e);
        }

        ControllerUtils.getInstance().addActivityLog("Enable statement language " + languageCode + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result disableStatementLanguage(long problemId, String languageCode) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        try {
            // TODO should check if language has been enabled
            if (!WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                return notFound();
            }

            problemService.disableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

            if (ProblemControllerUtils.getCurrentStatementLanguage().equals(languageCode)) {
                ProblemControllerUtils.setCurrentStatementLanguage(problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Statement language probably hasn't been added.", e);
        }

        ControllerUtils.getInstance().addActivityLog("Disable statement language " + languageCode + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result makeDefaultStatementLanguage(long problemId, String languageCode) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        try {
            // TODO should check if language has been enabled
            if (!WorldLanguageRegistry.getInstance().getLanguages().containsKey(languageCode)) {
                return notFound();
            }

            problemService.makeDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);
        } catch (IOException e) {
            throw new IllegalStateException("Statement language probably hasn't been added.", e);
        }

        ControllerUtils.getInstance().addActivityLog("Make statement language " + languageCode + " default of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> updateStatementForm, Problem problem, Set<String> allowedLanguages) {
        LazyHtml content = new LazyHtml(updateStatementView.render(updateStatementForm, problem.getId()));
        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemStatementController.updateStatementSwitchLanguage(problem.getId()));
        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.update"), routes.ProblemStatementController.updateStatement(problem.getId())));

        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListStatementMediaFiles(Form<UploadFileForm> uploadFileForm, Problem problem, List<FileInfo> mediaFiles, boolean isAllowedToUploadMediaFiles) {
        LazyHtml content = new LazyHtml(listStatementMediaFilesView.render(uploadFileForm, problem.getId(), mediaFiles, isAllowedToUploadMediaFiles));
        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.media.list"), routes.ProblemStatementController.listStatementMediaFiles(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Statement - List Media");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
