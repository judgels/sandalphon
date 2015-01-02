package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
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
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.controllers.authenticators.Secured;
import org.iatoki.judgels.sandalphon.forms.UpsertProblemForm;
import org.iatoki.judgels.sandalphon.views.html.problem.createView;
import org.iatoki.judgels.sandalphon.views.html.problem.listView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(Secured.class)
public final class ProblemController extends Controller {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProblemController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    public Result view(long id) {
        return redirect(routes.ProgrammingProblemController.view(id));
    }

    public Result update(long id) {
        return redirect(routes.ProgrammingProblemController.update(id));
    }

    public Result delete(long id) {
        return redirect(routes.ProblemController.index());
    }

    @Transactional
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<Problem> currentPage = problemService.pageProblem(page, 20, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.list"), new InternalLink(Messages.get("problem.create"), routes.ProblemController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<UpsertProblemForm> form = Form.form(UpsertProblemForm.class);
        return showCreate(form);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postCreate() {
        Form<UpsertProblemForm> form = Form.form(UpsertProblemForm.class).bindFromRequest();

        if (form.hasErrors()) {
            return showCreate(form);
        } else {
            UpsertProblemForm data = form.get();
            Problem problem = programmingProblemService.createProblem(data.name, data.note);

            return redirect(routes.ProgrammingProblemController.update(problem.getId()));
        }
    }

    private Result showCreate(Form<UpsertProblemForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.create"), routes.ProblemController.create())
        ), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
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
        content.appendLayout(c -> baseLayout.render("-", c));
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
