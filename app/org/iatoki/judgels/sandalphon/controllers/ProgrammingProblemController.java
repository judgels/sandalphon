package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.ProgrammingProblemStatement;
import org.iatoki.judgels.sandalphon.forms.UpdateGradingForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.views.html.programming.updateFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateGradingView;
import org.iatoki.judgels.sandalphon.views.html.programming.updateStatementView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.File;
import java.util.List;


public final class ProgrammingProblemController extends Controller {

    private final ProgrammingProblemService service;

    public ProgrammingProblemController(ProgrammingProblemService service) {
        this.service = service;
    }

    public Result update(long id) {
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @Transactional
    public Result updateStatement(long id) {
        ProgrammingProblemStatement programmingProblemStatement = service.getProblemStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", programmingProblemStatement.getStatement()));
        return showUpdateStatement(form, id);
    }

    @Transactional
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        service.updateProblemStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long id) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, id));
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programmingProblem.tab.general"), routes.ProgrammingProblemController.updateStatement(id))
        ), c));
        content.appendLayout(c -> headingLayout.render("problem.create", c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index()),
                new InternalLink("problem.create", routes.ProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    @Transactional
    public Result updateFiles(long id) {
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<String> filenames = service.getGradingFilenames(id);
        return showUpdateFiles(form, id, filenames);
    }

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

    @Transactional
    public Result updateGrading(long id) {
        Form<UpdateGradingForm> form = Form.form(UpdateGradingForm.class);
        List<String> filenames = service.getGradingFilenames(id);

        return showUpdateGrading(form, id, filenames);
    }

    @Transactional
    public Result postUpdateGrading(long id) {
        Form<UpdateGradingForm> form = Form.form(UpdateGradingForm.class).bindFromRequest();
        UpdateGradingForm data = form.get();

        service.updateProblemGrading(id, data.timeLimit, data.memoryLimit, data.tcIn, data.tcOut, data.subtaskBatches, data.subtaskPoints);

        return redirect(routes.ProgrammingProblemController.updateGrading(id));
    }

    private Result showUpdateGrading(Form<UpdateGradingForm> form, long id, List<String> gradingFilenames) {
        LazyHtml content = new LazyHtml(updateGradingView.render(form, id, gradingFilenames));
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programmingProblem.tab.general"), routes.ProgrammingProblemController.updateStatement(id)),
                new InternalLink(Messages.get("programmingProblem.tab.grading"), routes.ProgrammingProblemController.updateGrading(id)),
                new InternalLink(Messages.get("programmingProblem.tab.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        content.appendLayout(c -> headingLayout.render("problem.grading", c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index()),
                new InternalLink("problem.apasich", routes.ProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateFiles(Form<UpdateFilesForm> form, long id, List<String> filenames) {
        LazyHtml content = new LazyHtml(updateFilesView.render(form, id, filenames));
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programmingProblem.tab.general"), routes.ProgrammingProblemController.updateStatement(id)),
                new InternalLink(Messages.get("programmingProblem.tab.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        content.appendLayout(c -> headingLayout.render("problem.files", c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index()),
                new InternalLink("problem.create", routes.ProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private void appendTemplateLayout(LazyHtml content) {
        content.appendLayout(c -> leftSidebarLayout.render(ImmutableList.of(Html.apply("TODO")), c));
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
