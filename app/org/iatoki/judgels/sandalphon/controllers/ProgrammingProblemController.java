package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.*;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.controllers.authenticators.Secured;
import org.iatoki.judgels.sandalphon.forms.UpdateGradingForm;
import org.iatoki.judgels.sandalphon.forms.UpsertProblemForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateFilesForm;
import org.iatoki.judgels.sandalphon.forms.programming.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.views.html.programming.*;
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

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProgrammingProblemController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.viewGeneral(id));
    }

    @Transactional
    public Result viewGeneral(long id) {
        Problem problem = problemService.findProblemById(id);
        LazyHtml content = new LazyHtml(viewGeneralView.render(problem));
        appendViewTabsLayout(content, id, problem.getName());
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("programmingProblem.view.general"), routes.ProgrammingProblemController.viewGeneral(id))
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
        Problem problem = problemService.findProblemById(id);
        UpsertProblemForm content = new UpsertProblemForm();
        content.name = problem.getName();
        content.note = problem.getNote();
        Form<UpsertProblemForm> form = Form.form(UpsertProblemForm.class).fill(content);

        return showUpdateGeneral(form, id);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGeneral(long id) {
        Form<UpsertProblemForm> form = Form.form(UpsertProblemForm.class).bindFromRequest();
        problemService.updateProblem(id, form.get().name, form.get().note);
        return redirect(routes.ProgrammingProblemController.updateGeneral(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateStatement(long id) {
        Problem problem = problemService.findProblemById(id);
        String statement = programmingProblemService.getProblemStatement(id);
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
        form = form.bind(ImmutableMap.of("statement", statement));
        return showUpdateStatement(form, id, problem.getName());
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateStatement(long id) {
        Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
        programmingProblemService.updateProblemStatement(id, form.get().statement);
        return redirect(routes.ProgrammingProblemController.updateStatement(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateFiles(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateFilesForm> form = Form.form(UpdateFilesForm.class);
        List<String> filenames = programmingProblemService.getGradingFilenames(id);
        return showUpdateFiles(form, id, problem.getName(), filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateFiles(long id) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("file");

        if (file != null) {
            File evaluatorFile = file.getFile();

            programmingProblemService.uploadGradingFile(id, evaluatorFile, file.getFilename());
        }

        return redirect(routes.ProgrammingProblemController.updateFiles(id));
    }

    @AddCSRFToken
    @Transactional
    public Result updateGrading(long id) {
        Problem problem = problemService.findProblemById(id);
        Form<UpdateGradingForm> form = Form.form(UpdateGradingForm.class);
        List<String> filenames = programmingProblemService.getGradingFilenames(id);

        return showUpdateGrading(form, id, problem.getName(), filenames);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postUpdateGrading(long id) {
        Form<UpdateGradingForm> form = Form.form(UpdateGradingForm.class).bindFromRequest();
        UpdateGradingForm data = form.get();

        programmingProblemService.updateProblemGrading(id, data.timeLimit, data.memoryLimit, data.tcIn, data.tcOut, data.subtaskBatches, data.subtaskPoints);

        return redirect(routes.ProgrammingProblemController.updateGrading(id));
    }

    public Result delete(long id) {
        return TODO;
    }

    private Result showUpdateGeneral(Form<UpsertProblemForm> form, long id) {
        LazyHtml content = new LazyHtml(updateGeneralView.render(form, id));
        appendUpdateTabsLayout(content, id, form.get().name);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("programmingProblem.update.general"), routes.ProgrammingProblemController.updateGeneral(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, long id, String problemName) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, id));
        appendUpdateTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("programmingProblem.update.statement"), routes.ProgrammingProblemController.updateStatement(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateGrading(Form<UpdateGradingForm> form, long id, String problemName, List<String> gradingFilenames) {
        LazyHtml content = new LazyHtml(updateGradingView.render(form, id, gradingFilenames));
        appendUpdateTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("programmingProblem.update.grading"), routes.ProgrammingProblemController.updateGrading(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showUpdateFiles(Form<UpdateFilesForm> form, long id, String problemName, List<String> filenames) {
        LazyHtml content = new LazyHtml(updateFilesView.render(form, id, filenames));
        appendUpdateTabsLayout(content, id, problemName);
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("programmingProblem.update.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private void appendViewTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programmingProblem.view.tab.general"), routes.ProgrammingProblemController.viewGeneral(id))
        ), c));

        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.problem") + " #" + id + ": " + problemName, new InternalLink(Messages.get("problem.update"), routes.ProgrammingProblemController.update(id)), c));
    }

    private void appendUpdateTabsLayout(LazyHtml content, long id, String problemName) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("programmingProblem.update.tab.general"), routes.ProgrammingProblemController.updateGeneral(id)),
                new InternalLink(Messages.get("programmingProblem.update.tab.statement"), routes.ProgrammingProblemController.updateStatement(id)),
                new InternalLink(Messages.get("programmingProblem.update.tab.grading"), routes.ProgrammingProblemController.updateGrading(id)),
                new InternalLink(Messages.get("programmingProblem.update.tab.files"), routes.ProgrammingProblemController.updateFiles(id))
        ), c));

        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.problem") + " #" + id + ": " + problemName, new InternalLink(Messages.get("problem.view"), routes.ProgrammingProblemController.view(id)), c));
    }

    private void appendTemplateLayout(LazyHtml content) {
        content.appendLayout(c -> leftSidebarLayout.render(
                IdentityUtils.getUsername(),
                IdentityUtils.getUserRealName(),
                "#",
                org.iatoki.judgels.commons.controllers.routes.JophielClientController.logout(routes.Application.index().absoluteURL(request())).absoluteURL(request()),
                ImmutableList.of(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())),
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
