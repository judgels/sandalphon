package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.forms.CreateProblemForm;
import org.iatoki.judgels.sandalphon.views.html.problem.createProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewProblemView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

public final class ProblemController extends Controller {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result index() {
        LazyHtml content = new LazyHtml(Html.apply("TODO"));
        content.appendLayout(c -> headingWithActionLayout.render("problem.list", new InternalLink("problem.create", routes.ProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index())
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    public Result create() {
        Form<CreateProblemForm> form = Form.form(CreateProblemForm.class);
        return showCreate(form);
    }

    @Transactional
    public Result postCreate() {
        Form<CreateProblemForm> form = Form.form(CreateProblemForm.class).bindFromRequest();

        if (form.hasErrors()) {
            return showCreate(form);
        } else {
            CreateProblemForm data = form.get();
            problemService.createProblem(data.name, data.note);
            return redirect(routes.ProblemController.index());
        }
    }

    @Transactional
    public Result view(long id) {
        Problem problem = problemService.getProblem(id);
        LazyHtml content = new LazyHtml(viewProblemView.render(problem));
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink("problem.tab.preview", routes.ProblemController.index())
        ), c));
        content.appendLayout(c -> headingWithActionLayout.render(problem.getName(), new InternalLink("problem.update", routes.ProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index()),
                new InternalLink("problem.view", routes.ProblemController.view(id))
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private Result showCreate(Form<CreateProblemForm> form) {
        LazyHtml content = new LazyHtml(createProblemView.render(form));
        content.appendLayout(c -> headingLayout.render("problem.create", c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink("problem", routes.ProblemController.index()),
                new InternalLink("problem.create", routes.ProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
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
