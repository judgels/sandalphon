package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
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
import org.iatoki.judgels.gabriel.GradingMethod;
import org.iatoki.judgels.gabriel.GradingMethodRegistry;
import org.iatoki.judgels.gabriel.grading.batch.SubtaskBatchGradingConf;
import org.iatoki.judgels.sandalphon.ProgrammingProblem;
import org.iatoki.judgels.sandalphon.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.ProgrammingProblemUtils;
import org.iatoki.judgels.sandalphon.controllers.authenticators.Secured;
import org.iatoki.judgels.sandalphon.forms.grading.SubtaskBatchGradingForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpsertForm;
import org.iatoki.judgels.sandalphon.views.html.grading.batch.batchGradingView;
import org.iatoki.judgels.sandalphon.views.html.programming.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGeneralView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateStatementView;
import org.iatoki.judgels.sandalphon.views.html.programming.viewGeneralView;
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

@Security.Authenticated(Secured.class)
public final class ProgrammingProblemController extends Controller {

    private final ProgrammingProblemService service;

    public ProgrammingProblemController(ProgrammingProblemService service) {
        this.service = service;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Transactional
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<ProgrammingProblem> currentPage = service.pageProblem(page, 20, sortBy, orderBy, filterString);

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
            ProgrammingProblem problem = service.createProblem(data.name, data.gradingMethod, data.note);

            return redirect(routes.ProgrammingProblemController.update(problem.getId()));
        }
    }

    private Result showCreate(Form<UpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form, GradingMethodRegistry.getInstance().getGradingMethods()));
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
        ProgrammingProblem problem = service.findProblemById(id);
        LazyHtml content = new LazyHtml(viewGeneralView.render(problem));
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
        ProgrammingProblem problem = service.findProblemById(id);
        UpsertForm content = new UpsertForm();
        content.name = problem.getName();
        content.note = problem.getNote();
        Form<UpsertForm> form = Form.form(UpsertForm.class).fill(content);

        return showUpdateGeneral(form, id);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGeneral(long id) {
        Form<UpsertForm> form = Form.form(UpsertForm.class).bindFromRequest();
        service.updateProblem(id, form.get().name, form.get().note);
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateStatement(long id) {
        ProgrammingProblem problem = service.findProblemById(id);
        String statement = service.getProblemStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));
        return showUpdateStatement(form, id, problem.getName());
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        service.updateProblemStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateFiles(long id) {
        ProgrammingProblem problem = service.findProblemById(id);
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<String> filenames = service.getGradingFilenames(id);
        return showUpdateFiles(form, id, problem.getName(), filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateFiles(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            File evaluatorFile = file.getFile();

            service.uploadGradingFile(id, evaluatorFile, file.getFilename());
        }

        return redirect(routes.ProgrammingProblemController.updateFiles(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGrading(long id) {
        ProgrammingProblem problem = service.findProblemById(id);
        String json = service.getProblemGrading(id);

        Gson gson = new Gson();
        SubtaskBatchGradingConf conf = gson.fromJson(json, SubtaskBatchGradingConf.class);

        System.out.println("aslinya " + json);
        System.out.println("WUW " + gson.toJson(conf));
        System.out.println("YEY " + gson.toJson(ProgrammingProblemUtils.toGradingForm(conf)));

        Form<SubtaskBatchGradingForm> form = Form.form(SubtaskBatchGradingForm.class).fill(ProgrammingProblemUtils.toGradingForm(conf));

        List<String> filenames = service.getGradingFilenames(id);

        return showUpdateGrading(form, problem, filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGrading(long id) {
        Form<SubtaskBatchGradingForm> form = Form.form(SubtaskBatchGradingForm.class).bindFromRequest();
        SubtaskBatchGradingForm data = form.get();

        System.out.println("NUL GAK SICH " + data.testSetsSubtasks);

        Gson gson = new Gson();

        System.out.println("?SDFSD ? " + gson.toJson(data));

        SubtaskBatchGradingConf conf = ProgrammingProblemUtils.toGradingConf(data);

        System.out.println("JADINYA = " + gson.toJson(conf));

        service.updateProblemGrading(id, gson.toJson(conf));

        return redirect(routes.ProgrammingProblemController.updateGrading(id));
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

    private Result showUpdateGrading(Form<SubtaskBatchGradingForm> form, ProgrammingProblem problem, List<String> gradingFilenames) {

        GradingMethod gradingMethod = problem.getGradingMethod();

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
                new InternalLink(Messages.get("problem.programming.view.tab.general"), routes.ProgrammingProblemController.viewGeneral(id))
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
