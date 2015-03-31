package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.ProblemType;
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
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;

import org.iatoki.judgels.sandalphon.views.html.listView;
import org.iatoki.judgels.sandalphon.views.html.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.viewClientProblemView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
        return list(0, "timeUpdate", "desc", "");
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result list(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Problem> problems = problemService.pageProblems(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(problems, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.list"), new InternalLink(Messages.get("commons.create"), routes.ProgrammingProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToGeneral(long problemId) {
        return redirect(routes.ProblemController.view(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToStatement(long problemId) {
        return redirect(routes.ProblemController.viewStatement(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result jumpToClients(long problemId) {
        return redirect(routes.ProblemController.updateClientProblems(problemId));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result view(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (problem.getType() == ProblemType.PROGRAMMING) {
            return redirect(routes.ProgrammingProblemController.viewGeneral(problemId));
        } else {
            return badRequest();
        }
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result viewStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (problem.getType() == ProblemType.PROGRAMMING) {
            return redirect(routes.ProgrammingProblemController.viewStatement(problemId));
        } else {
            return badRequest();
        }
    }

    @AddCSRFToken
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result updateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        String statement = problemService.getStatement(problem.getJid());
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));

        return showUpdateStatement(form, problem.getId(), problem.getName());
    }

    @RequireCSRFCheck
    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result postUpdateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdateStatement(form, problem.getId(), problem.getName());
        } else {
            problemService.updateStatement(problemId, form.get().statement);
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
        List<File> mediaFiles = problemService.getStatementMediaFiles(problem.getJid());

        return showListStatementMediaFiles(form, problem.getId(), problem.getName(), mediaFiles);
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
            problemService.uploadStatementMediaFile(problem.getId(), mediaFile, file.getFilename());
            return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            File mediaFile = file.getFile();
            problemService.uploadStatementMediaFileZipped(problem.getId(), mediaFile);
            return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
        }

        return redirect(routes.ProblemController.listStatementMediaFiles(problem.getId()));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Authorized("writer")
    public Result downloadStatementMediaFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        File file = problemService.getStatementMediaFile(problem.getJid(), filename);
        if (file.exists()) {
            return downloadFile(file);
        } else {
            return notFound();
        }
    }


    public Result renderMediaByJid(String problemJid, String imageFilename) {
        File image = problemService.getStatementMediaFile(problemJid, imageFilename);
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
        } catch (IOException e) {
            return internalServerError();
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
            appendTabsLayout(content, problemId, problem.getName());
            content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                    new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                    new InternalLink(Messages.get("problem.client.client"), routes.ProblemController.viewClientProblem(problemId, clientProblemId))
            ), c));
            appendTemplateLayout(content);

            return lazyOk(content);
        } else {
            return redirect(routes.ProgrammingProblemController.updateGeneral(problem.getId()));
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

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long problemId, String problemName) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, problemId));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewStatement(problemId)),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problemId))
        ), c));
        appendTabsLayout(content, problemId, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.update"), routes.ProblemController.updateStatement(problemId))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showListStatementMediaFiles(Form<UploadFileForm> form, long problemId, String problemName, List<File> mediaFiles) {
        LazyHtml content = new LazyHtml(listStatementMediaFilesView.render(form, problemId, mediaFiles));
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewStatement(problemId)),
                new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.media"), routes.ProblemController.listStatementMediaFiles(problemId))
                ), c));
        appendTabsLayout(content, problemId, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problemId)),
                new InternalLink(Messages.get("problem.statement.media.list"), routes.ProblemController.listStatementMediaFiles(problemId))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdateClientProblems(Form<ClientProblemUpsertForm> form, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemsView.render(form, problem.getId(), clients, clientProblems));
        appendTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId())),
                new InternalLink(Messages.get("problem.client.list"), routes.ProblemController.updateClientProblems(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private void appendTabsLayout(LazyHtml content, long problemId, String problemName) {
        Problem problem = problemService.findProblemById(problemId);

        ImmutableList.Builder<InternalLink> internalLinksBuilder = ImmutableList.builder();
        internalLinksBuilder.add(
                new InternalLink(Messages.get("problem.general"), routes.ProblemController.jumpToGeneral(problemId)),
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problemId))
        );

        if (problem.getType() == ProblemType.PROGRAMMING) {
            internalLinksBuilder.add(
                    new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problemId)),
                    new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problemId))
            );
        }

        internalLinksBuilder.add(
                new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problemId))
        );

        content.appendLayout(c -> tabLayout.render(internalLinksBuilder.build(), c));

        content.appendLayout(c -> headingLayout.render("#" + problemId + ": " + problemName, c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();

        if (isAllowedToWrite()) {
            internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()));
        }

        if (isAdmin()) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("userRole.userRoles"), routes.UserRoleController.index()));
        }

        content.appendLayout(c -> sidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(routes.ProblemController.index().absoluteURL(request())).absoluteURL(request()),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(request())).absoluteURL(request()),
                        internalLinkBuilder.build(), c)
        );
        content.appendLayout(c -> headerFooterLayout.render(c));
        content.appendLayout(c -> baseLayout.render("TODO", c));
    }

    private Result downloadFile(File file) {
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
        return ok(file);
    }

    private Result lazyOk(LazyHtml content) {
        return getResult(content, Http.Status.OK);
    }

    private boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    private boolean isWriter() {
        return SandalphonUtils.hasRole("writer");
    }

    private boolean isAllowedToWrite() {
        return isAdmin() || isWriter();
    }

    private Result getResult(LazyHtml content, int statusCode) {
        switch (statusCode) {
            case Http.Status.OK:
                return ok(content.render(0));
            case Http.Status.NOT_FOUND:
                return notFound(content.render(0));
            default:
                return badRequest(content.render(0));
        }
    }
}
