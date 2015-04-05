package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.forms.ProblemCreateForm;
import org.iatoki.judgels.sandalphon.forms.ProblemUpdateForm;
import org.iatoki.judgels.sandalphon.forms.VersionCommitForm;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.forms.UpdateStatementForm;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;

import org.iatoki.judgels.sandalphon.views.html.problem.listProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.createProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.updateProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.problem.listStatementLanguagesView;
import org.iatoki.judgels.sandalphon.views.html.problem.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewClientProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.listVersionsView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewVersionLocalChangesView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
public final class ProblemController extends Controller {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final ClientService clientService;
    private final GraderService graderService;

    public ProblemController(ProblemService problemService, ClientService clientService, GraderService graderService) {
        this.problemService = problemService;
        this.clientService = clientService;
        this.graderService = graderService;
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result index() {
        return listProblems(0, "timeUpdate", "desc", "");
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listProblems(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Problem> problems = problemService.pageProblems(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listProblemsView.render(problems, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.list"), new InternalLink(Messages.get("commons.create"), routes.ProblemController.createProblem()), c));

        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problems");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result createProblem() {
        Form<ProblemCreateForm> form = Form.form(ProblemCreateForm.class);
        return showCreateProblem(form);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postCreateProblem() {
        Form<ProblemCreateForm> form = Form.form(ProblemCreateForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreateProblem(form);
        } else {
            ProblemCreateForm data = form.get();

            if (data.type.equals(ProblemType.PROGRAMMING.name())) {
                ProblemControllerUtils.setJustCreatedProblem(data.name, data.additionalNote, data.initLanguageCode);
                return redirect(routes.ProgrammingProblemController.createProgrammingProblem());
            }

            return internalServerError();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result enterProblem(long problemId) {
        return redirect(routes.ProblemController.jumpToStatement(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToStatement(long problemId) {
        return redirect(routes.ProblemController.viewStatement(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToVersions(long problemId) {
        return redirect(routes.ProblemController.viewVersionLocalChanges(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToClients(long problemId) {
        return redirect(routes.ProblemController.updateClientProblems(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewProblem(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        LazyHtml content = new LazyHtml(viewProblemView.render(problem));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateProblem(problem.getId()))
        ), c));
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.view"), routes.ProblemController.viewProblem(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateProblem(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        ProblemUpdateForm data = new ProblemUpdateForm();
        data.name = problem.getName();
        data.additionalNote = problem.getAdditionalNote();

        Form<ProblemUpdateForm> form = Form.form(ProblemUpdateForm.class).fill(data);
        return showUpdateProblem(form, problem);
    }


    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateProblem(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ProblemUpdateForm> form = Form.form(ProblemUpdateForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateProblem(form, problem);
        } else {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            ProblemUpdateForm data = form.get();
            problemService.updateProblem(problemId, data.name, data.additionalNote);
            return redirect(routes.ProblemController.viewProblem(problem.getId()));
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (problem.getType() == ProblemType.PROGRAMMING) {
            return redirect(routes.ProgrammingProblemController.viewStatement(problem.getId()));
        } else {
            return badRequest();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        return redirect(routes.ProblemController.viewStatement(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        return redirect(routes.ProblemController.updateStatement(problemId));
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateStatement(long problemId) {
        ProblemControllerUtils.establishStatementLanguage(problemService, problemId);

        Problem problem = problemService.findProblemById(problemId);

        String statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());

        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));

        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());
        List<String> allowedLanguages = availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toList());


        return showUpdateStatement(form, problem, allowedLanguages);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateStatement(long problemId) {
        ProblemControllerUtils.establishStatementLanguage(problemService, problemId);

        Problem problem = problemService.findProblemById(problemId);

        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());
            List<String> allowedLanguages = availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toList());
            return showUpdateStatement(form, problem, allowedLanguages);
        } else {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.updateStatement(IdentityUtils.getUserJid(), problemId, ProblemControllerUtils.getCurrentStatementLanguage(), form.get().statement);
            return redirect(routes.ProblemController.updateStatement(problem.getId()));
        }
    }

    public Result renderMediaById(long problemId, String imageFilename) {
        Problem problem = problemService.findProblemById(problemId);
        return renderMediaByJid(problem.getJid(), imageFilename);
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listStatementMediaFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Form<UploadFileForm> form = Form.form(UploadFileForm.class);
        List<FileInfo> mediaFiles = problemService.getStatementMediaFiles(IdentityUtils.getUserJid(), problem.getJid());

        return showListStatementMediaFiles(form, problem, mediaFiles);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUploadStatementMediaFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.uploadStatementMediaFile(IdentityUtils.getUserJid(), problem.getId(), mediaFile, file.getFilename());

            return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.uploadStatementMediaFileZipped(IdentityUtils.getUserJid(), problem.getId(), mediaFile);

            return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
        }

        return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result downloadStatementMediaFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        String mediaURL = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File mediaFile = new File(mediaURL);
            return downloadFile(mediaFile);
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listStatementLanguages(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());
        String defaultLanguage = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());

        LazyHtml content = new LazyHtml(listStatementLanguagesView.render(availableLanguages, defaultLanguage, problem.getId()));

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewStatement(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language"), routes.ProblemController.listStatementLanguages(problem.getId()))
        ), c));
        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language.list"), routes.ProblemController.listStatementLanguages(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Statement Languages");

        return ControllerUtils.getInstance().lazyOk(content);
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postAddStatementLanguage(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        problemService.addLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

        return redirect(routes.ProblemController.listStatementLanguages(problem.getId()));
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result enableStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        problemService.enableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

        return redirect(routes.ProblemController.listStatementLanguages(problem.getId()));
    }


    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result disableStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        problemService.disableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

        if (ProblemControllerUtils.getCurrentStatementLanguage().equals(languageCode)) {
            ProblemControllerUtils.setCurrentStatementLanguage(problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid()));
        }
        return redirect(routes.ProblemController.listStatementLanguages(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result makeDefaultStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        problemService.makeDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

        return redirect(routes.ProblemController.listStatementLanguages(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result listVersionHistory(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        List<GitCommit> versions = problemService.getVersions(IdentityUtils.getUserJid(), problem.getJid());
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        LazyHtml content = new LazyHtml(listVersionsView.render(versions, problem.getId(), isClean));

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.version.local"), routes.ProblemController.viewVersionLocalChanges(problem.getId())),
                new InternalLink(Messages.get("problem.version.history"), routes.ProblemController.listVersionHistory(problem.getId()))
        ), c));

        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.version"), routes.ProblemController.jumpToVersions(problem.getId())),
                new InternalLink(Messages.get("problem.version.history"), routes.ProblemController.listVersionHistory(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - History");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result restoreVersionHistory(long problemId, String hash) {
        Problem problem = problemService.findProblemById(problemId);

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        problemService.restore(problem.getJid(), hash);

        return redirect(routes.ProblemController.listVersionHistory(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        Form<VersionCommitForm> form = Form.form(VersionCommitForm.class);

        return showViewVersionLocalChanges(form, problem, isClean);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postCommitVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Form<VersionCommitForm> form = Form.form(VersionCommitForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
            return showViewVersionLocalChanges(form, problem, isClean);
        }

        VersionCommitForm data = form. get();

        if (problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
            flash("localChangesError", Messages.get("problem.version.local.cantCommit"));
        } else if (!problemService.commitThenMergeUserClone(IdentityUtils.getUserJid(), problem.getJid(), data.title, data.description)) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        } else if (!problemService.pushUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        } else {
            problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());
        }

        return redirect(routes.ProblemController.viewVersionLocalChanges(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid());

        if (!problemService.updateUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        }
        return redirect(routes.ProblemController.viewVersionLocalChanges(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result discardVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());

        return redirect(routes.ProblemController.viewVersionLocalChanges(problem.getId()));
    }

    public Result renderMediaByJid(String problemJid, String imageFilename) {
        String mediaURL = problemService.getStatementMediaFileURL(null, problemJid, imageFilename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File image = new File(mediaURL);

            if (!image.exists()) {
                return notFound();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            response().setHeader("Cache-Control", "no-transform,public,max-age=300,s-maxage=900");
            response().setHeader("Last-Modified", sdf.format(new Date(image.lastModified())));

            try {
                BufferedImage in = ImageIO.read(image);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                String type = FilenameUtils.getExtension(image.getAbsolutePath());

                ImageIO.write(in, type, baos);
                return ok(baos.toByteArray()).as("image/" + type);
            } catch (IOException e2) {
                return internalServerError();
            }
        }
    }


    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateClientProblems(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
        List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
        List<Client> clients = clientService.findAllClients();

        return showUpdateClientProblems(form, problem, clients, clientProblems);
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateClientProblems(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
            List<Client> clients = clientService.findAllClients();
            return showUpdateClientProblems(form, problem, clients, clientProblems);
        } else {
            ClientProblemUpsertForm clientProblemUpsertForm = form.get();
            if ((clientService.existsByJid(clientProblemUpsertForm.clientJid)) && (!clientService.isClientProblemInProblemByClientJid(problem.getJid(), clientProblemUpsertForm.clientJid))) {
                clientService.createClientProblem(problem.getJid(), clientProblemUpsertForm.clientJid);
                return redirect(routes.ProblemController.updateClientProblems(problem.getId()));
            } else {
                List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemId(problem.getJid());
                List<Client> clients = clientService.findAllClients();
                return showUpdateClientProblems(form, problem, clients, clientProblems);
            }
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewClientProblem(long problemId, long clientProblemId) {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            LazyHtml content = new LazyHtml(viewClientProblemView.render(problem, clientProblem));
            ProblemControllerUtils.appendTabsLayout(content, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                    new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                    new InternalLink(Messages.get("problem.client.client"), routes.ProblemController.viewClientProblem(problemId, clientProblemId))
            ));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    public Result verifyProblem() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String clientJid = form.get("clientJid");
        String clientSecret = form.get("clientSecret");
        if (clientService.existsByJid(clientJid)) {
            Client client = clientService.findClientByJid(clientJid);
            if (client.getSecret().equals(clientSecret)) {
                String problemJid = form.get("problemJid");
                if (problemService.problemExistsByJid(problemJid)) {
                    return ok(problemService.findProblemByJid(problemJid).getName());
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

    private Result showCreateProblem(Form<ProblemCreateForm> form) {
        LazyHtml content = new LazyHtml(createProblemView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.create"), routes.ProblemController.createProblem())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateProblem(Form<ProblemUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateProblemView.render(form, problem));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateProblem(problem.getId()))
                ), c));
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.update"), routes.ProblemController.updateProblem(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, Problem problem, List<String> allowedLanguages) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, problem.getId()));
        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemController.updateStatementSwitchLanguage(problem.getId()));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewStatement(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language"), routes.ProblemController.listStatementLanguages(problem.getId()))
        ), c));
        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.update"), routes.ProblemController.updateStatement(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListStatementMediaFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> mediaFiles) {
        LazyHtml content = new LazyHtml(listStatementMediaFilesView.render(form, problem.getId(), mediaFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewStatement(problem.getId())),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language"), routes.ProblemController.listStatementLanguages(problem.getId()))
        ), c));
        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())),
                new InternalLink(Messages.get("problem.statement.language.list"), routes.ProblemController.listStatementMediaFiles(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showViewVersionLocalChanges(Form<VersionCommitForm> form, Problem problem, boolean isClean) {
        LazyHtml content = new LazyHtml(viewVersionLocalChangesView.render(form, problem, isClean));

        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.version.local"), routes.ProblemController.viewVersionLocalChanges(problem.getId())),
                new InternalLink(Messages.get("problem.version.history"), routes.ProblemController.listVersionHistory(problem.getId()))
        ), c));

        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.version"), routes.ProblemController.jumpToVersions(problem.getId())),
                new InternalLink(Messages.get("problem.version.local"), routes.ProblemController.viewVersionLocalChanges(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - Local Changes");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateClientProblems(Form<ClientProblemUpsertForm> form, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemsView.render(form, problem.getId(), clients, clientProblems));
        ProblemControllerUtils.appendTabsLayout(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(problem.getName(), routes.ProblemController.viewProblem(problem.getId())),
                new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId())),
                new InternalLink(Messages.get("problem.client.list"), routes.ProblemController.updateClientProblems(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result downloadFile(File file) {
        if (!file.exists()) {
            return notFound();
        }
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
        return ok(file);
    }
}
