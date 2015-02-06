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
import org.iatoki.judgels.sandalphon.GraderClient;
import org.iatoki.judgels.sandalphon.GraderClientService;
import org.iatoki.judgels.sandalphon.GraderClientUpsertForm;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.graderclient.createView;
import org.iatoki.judgels.sandalphon.views.html.graderclient.listView;
import org.iatoki.judgels.sandalphon.views.html.graderclient.updateView;
import org.iatoki.judgels.sandalphon.views.html.graderclient.viewView;
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
public final class GraderClientController extends Controller {

    private static final long PAGE_SIZE = 20;

    private GraderClientService clientService;

    public GraderClientController(GraderClientService clientService) {
        this.clientService = clientService;
    }

    @Transactional
    public Result index() {
        return list(0, "id", "asc", "");
    }

    private Result showCreate(Form<GraderClientUpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("graderclient.clients"), routes.GraderClientController.index()),
                new InternalLink(Messages.get("graderclient.create"), routes.GraderClientController.create())
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<GraderClientUpsertForm> form = Form.form(GraderClientUpsertForm.class);

        return showCreate(form);
    }

    @RequireCSRFCheck
    @Transactional
    public Result postCreate() {
        Form<GraderClientUpsertForm> form = Form.form(GraderClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            GraderClientUpsertForm clientUpsertForm = form.get();

            clientService.createGraderClient(clientUpsertForm.name);

            return redirect(routes.GraderClientController.index());
        }
    }

    @Transactional
    public Result view(long clientId) {
        GraderClient client = clientService.findGraderClientById(clientId);
        LazyHtml content = new LazyHtml(viewView.render(client));
        content.appendLayout(c -> headingWithActionLayout.render(client.getName(), new InternalLink(Messages.get("graderclient.update"), routes.GraderClientController.update(clientId)), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("graderclient.clients"), routes.GraderClientController.index()),
                new InternalLink(Messages.get("graderclient.view"), routes.GraderClientController.view(clientId))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdate(Form<GraderClientUpsertForm> form, GraderClient client) {
        LazyHtml content = new LazyHtml(updateView.render(form, client.getId()));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.update"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.GraderClientController.index()),
                new InternalLink(Messages.get("client.update"), routes.GraderClientController.update(client.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    @Transactional
    public Result update(long clientId) {
        GraderClient client = clientService.findGraderClientById(clientId);
        GraderClientUpsertForm clientUpsertForm = new GraderClientUpsertForm();
        clientUpsertForm.name = client.getName();
        Form<GraderClientUpsertForm> form = Form.form(GraderClientUpsertForm.class).fill(clientUpsertForm);

        return showUpdate(form, client);
    }

    @Transactional
    public Result postUpdate(long clientId) {
        GraderClient client = clientService.findGraderClientById(clientId);
        Form<GraderClientUpsertForm> form = Form.form(GraderClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors()) {
            return showUpdate(form, client);
        } else {
            GraderClientUpsertForm clientUpsertForm = form.get();

            clientService.updateGraderClient(clientId, clientUpsertForm.name);

            return redirect(routes.GraderClientController.index());
        }
    }

    @Transactional
    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<GraderClient> currentPage = clientService.pageGraderClient(page, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("graderclient.list"), new InternalLink(Messages.get("graderclient.create"), routes.GraderClientController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("graderclient.clients"), routes.GraderClientController.index())
        ), c));
        appendTemplateLayout(content);

        return lazyOk(content);
    }

    private void appendTemplateLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();
        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProgrammingProblemController.index()));

        if (SandalphonUtils.hasRole("admin")) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("graderclient.clients"), routes.GraderClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("userRole.userRoles"), routes.UserRoleController.index()));
        }

        content.appendLayout(c -> leftSidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        "#",
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