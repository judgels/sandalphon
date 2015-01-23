package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.gabriel.Grader;
import org.iatoki.judgels.gabriel.GraderRegistry;
import org.iatoki.judgels.gabriel.GradingType;
import org.iatoki.judgels.gabriel.graders.BatchGradingConfig;
import org.iatoki.judgels.sandalphon.programming.Problem;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.programming.ProblemUtils;
import org.iatoki.judgels.sandalphon.controllers.authenticators.Secured;
import org.iatoki.judgels.sandalphon.forms.grading.BatchGradingConfigForm;
import org.iatoki.judgels.sandalphon.forms.programming.SubmitForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpsertForm;
import org.iatoki.judgels.sandalphon.programming.Submission;
import org.iatoki.judgels.sandalphon.views.html.grading.batch.batchGradingView;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewSubmissionsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.io.File;
import java.util.List;
import java.util.Map;

@Security.Authenticated(Secured.class)
public final class ProgrammingProblemController extends Controller {

    private final ProblemService service;

    public ProgrammingProblemController(ProblemService service) {
        this.service = service;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Transactional
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<Problem> currentPage = service.pageProblem(page, 20, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.programming.list"), new InternalLink(Messages.get("problem.programming.create"), routes.ProgrammingProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<UpsertForm> form = Form.form(UpsertForm.class);
        return showCreate(form);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postCreate() {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();

        if (form.hasErrors()) {
            return showCreate(form);
        } else {
            UpsertForm data = form.get();
            Problem problem = service.createProblem(data.name, data.gradingType, data.additionalNote);

            return redirect(routes.ProgrammingProblemController.update(problem.getId()));
        }
    }

    private Result showCreate(Form<UpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.programming.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.create"), routes.ProgrammingProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.viewGeneral(id));
    }

    @Transactional
    public Result viewGeneral(long id) {
        Problem problem = service.findProblemById(id);
        LazyHtml content = new LazyHtml(viewGeneralView.render(problem));
        appendViewTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Transactional
    public Result viewStatement(long id) {
        String statement = service.getStatement(id);
        Problem problem = service.findProblemById(id);

        Form<SubmitForm> form = Form.form(SubmitForm.class);
        LazyHtml content = new LazyHtml(viewStatementView.render(form, statement, id));
        appendViewTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Transactional
    public Result viewSubmissions(long id) {
        Problem problem = service.findProblemById(id);
        Page<Submission> submissions = service.pageSubmission(0, 20, "id", "asc", problem.getJid());
        LazyHtml content = new LazyHtml(viewSubmissionsView.render(submissions, "id", "asc", problem.getJid()));
        appendViewTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    public Result update(long id) {
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGeneral(long id) {
        Problem problem = service.findProblemById(id);
        UpsertForm content = new UpsertForm();
        content.name = problem.getName();
        content.additionalNote = problem.getAdditionalNote();
        Form<UpsertForm> form = Form.form(UpsertForm.class).fill(content);

        return showUpdateGeneral(form, id);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGeneral(long id) {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();
        service.updateProblem(id, form.get().name, form.get().additionalNote);
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateStatement(long id) {
        Problem problem = service.findProblemById(id);
        String statement = service.getStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));
        return showUpdateStatement(form, id, problem.getName());
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        service.updateStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateFiles(long id) {
        Problem problem = service.findProblemById(id);
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<String> filenames = service.getTestDataFilenames(id);
        return showUpdateFiles(form, id, problem.getName(), filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateFiles(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            File evaluatorFile = file.getFile();

            service.uploadTestDataFile(id, evaluatorFile, file.getFilename());
        }

        return redirect(routes.ProgrammingProblemController.updateFiles(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGrading(long id) {
        Problem problem = service.findProblemById(id);
        String json = service.getGradingConfig(id);

        Gson gson = new Gson();
        BatchGradingConfig config = gson.fromJson(json, BatchGradingConfig.class);

        Form<BatchGradingConfigForm> form = Form.form(BatchGradingConfigForm.class).fill(ProblemUtils.toGradingForm(config));

        List<String> filenames = service.getTestDataFilenames(id);

        return showUpdateGrading(form, problem, filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGrading(long id) {
        Form<BatchGradingConfigForm> form = Form.form(BatchGradingConfigForm.class).bindFromRequest();
        BatchGradingConfigForm data = form.get();


        Gson gson = new Gson();
        BatchGradingConfig config = ProblemUtils.toGradingConfig(data);

        service.updateGradingConfig(id, gson.toJson(config));

        return redirect(routes.ProgrammingProblemController.updateGrading(id));
    }

    @RequireCSRFCheck
    @Transactional
    public Result postSubmit(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            try {
                File sourceFile = file.getFile();

                byte[] sourceFileData = FileUtils.readFileToByteArray(sourceFile);
                Map<String, byte[]> sourceFiles = ImmutableMap.of(file.getFilename(), sourceFileData);

                service.submit(id, sourceFiles);
                return ok("dah disubmit");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return redirect(routes.ProgrammingProblemController.viewStatement(id));
    }

    public Result delete(long id) {
        return TODO;
    }

    private Result showUpdateGeneral(Form<UpsertForm> form, long id) {
        LazyHtml content = new LazyHtml(updateGeneralView.render(form, id));
        appendUpdateTabsLayout(content, id, form.get().name);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.general"), routes.ProgrammingProblemController.updateGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long id, String problemName) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, id));
        appendUpdateTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.statement"), routes.ProgrammingProblemController.updateStatement(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateGrading(Form<BatchGradingConfigForm> form, Problem problem, List<String> gradingFilenames) {
        LazyHtml content = new LazyHtml(batchGradingView.render(form, problem, gradingFilenames));
        appendUpdateTabsLayout(content, problem.getId(), problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.grading"), routes.ProgrammingProblemController.updateGrading(problem.getId()))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateFiles(Form<UpdateFilesForm> form, long id, String problemName, List<String> filenames) {
        LazyHtml content = new LazyHtml(updateFilesView.render(form, id, filenames));
        appendUpdateTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.problems"), routes.ProgrammingProblemController.index()),
                new InternalLink(Messages.get("problem.programming.update.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private void appendViewTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.view.tab.general"), routes.ProgrammingProblemController.viewGeneral(id)),
                new InternalLink(Messages.get("problem.programming.view.tab.statement"), routes.ProgrammingProblemController.viewStatement(id)),
                new InternalLink(Messages.get("problem.programming.view.tab.submission"), routes.ProgrammingProblemController.viewSubmissions(id))
        ), c));

        content.appendLayout(c -> headingWithActionLayout.render("#" + id + ": " + problemName, new InternalLink(Messages.get("problem.programming.update"), routes.ProgrammingProblemController.update(id)), c));
    }

    private void appendUpdateTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.update.tab.general"), routes.ProgrammingProblemController.updateGeneral(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.statement"), routes.ProgrammingProblemController.updateStatement(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.grading"), routes.ProgrammingProblemController.updateGrading(id)),
                new InternalLink(Messages.get("problem.programming.update.tab.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));

        content.appendLayout(c -> headingWithActionLayout.render("#" + id + ": " + problemName, new InternalLink(Messages.get("problem.programming.view"), routes.ProgrammingProblemController.view(id)), c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        content.appendLayout(c -> leftSidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        "#",
                        org.iatoki.judgels.commons.controllers.routes.JophielClientController.logout(routes.Application.index().absoluteURL(request())).absoluteURL(request()),
                        ImmutableList.of(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index())),
                        c)
        );
        content.appendLayout(c -> headerFooterLayout.render(c));
        content.appendLayout(c -> baseLayout.render("TODO", c));
    }

    private Result lazyOk(LazyHtml content) {
        return getResult(content, Http.Status.OK);
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
