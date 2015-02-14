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
import org.iatoki.judgels.sandalphon.Grader;
import org.iatoki.judgels.sandalphon.GraderService;
import org.iatoki.judgels.sandalphon.GraderUpsertForm;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.grader.createView;
import org.iatoki.judgels.sandalphon.views.html.grader.listView;
import org.iatoki.judgels.sandalphon.views.html.grader.updateView;
import org.iatoki.judgels.sandalphon.views.html.grader.viewView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Authorized(value = {"admin"})
public final class GraderController extends Controller {

    private static final long PAGE_SIZE = 20;

    private GraderService graderService;

    public GraderController(GraderService graderService) {
        this.graderService = graderService;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    private Result showCreate(Form<GraderUpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("grader.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.create"), routes.GraderController.create())
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class);

        return showCreate(form);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postCreate() {
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            GraderUpsertForm clientUpsertForm = form.get();
            graderService.createGrader(clientUpsertForm.name);

            return redirect(routes.GraderController.index());
        }
    }

    @Transactional
    public Result view(long graderId) {
        Grader grader = graderService.findGraderById(graderId);
        LazyHtml content = new LazyHtml(viewView.render(grader));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("grader.grader") + " #" + grader.getId() + ": " + grader.getName(), new InternalLink(Messages.get("commons.update"), routes.GraderController.update(graderId)), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.view"), routes.GraderController.view(graderId))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdate(Form<GraderUpsertForm> form, Grader grader) {
        LazyHtml content = new LazyHtml(updateView.render(form, grader.getId()));
        content.appendLayout(c -> headingLayout.render(Messages.get("grader.grader") + " #" + grader.getId() + ": " + grader.getName(), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.update"), routes.GraderController.update(grader.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    @Transactional
    public Result update(long graderId) {
        Grader grader = graderService.findGraderById(graderId);
        GraderUpsertForm clientUpsertForm = new GraderUpsertForm();
        clientUpsertForm.name = grader.getName();
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).fill(clientUpsertForm);

        return showUpdate(form, grader);
    }

    @Transactional
    public Result postUpdate(long graderId) {
        Grader grader = graderService.findGraderById(graderId);
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdate(form, grader);
        } else {
            GraderUpsertForm clientUpsertForm = form.get();
            graderService.updateGrader(graderId, clientUpsertForm.name);

            return redirect(routes.GraderController.index());
        }
    }

    @Transactional
    public Result list(long pageIndex, String orderBy, String orderDir, String filterString) {
        Page<Grader> graders = graderService.pageGraders(pageIndex, PAGE_SIZE, orderBy, orderDir, filterString);

        LazyHtml content = new LazyHtml(listView.render(graders, orderBy, orderDir, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("grader.list"), new InternalLink(Messages.get("commons.create"), routes.GraderController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();
        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index()));

        if (SandalphonUtils.hasRole("admin")) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("userRole.userRoles"), routes.UserRoleController.index()));
        }

        content.appendLayout(c -> leftSidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(routes.GraderController.index().absoluteURL(request())).absoluteURL(request()),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(request())).absoluteURL(request()),
                        internalLinkBuilder.build(), c)
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
