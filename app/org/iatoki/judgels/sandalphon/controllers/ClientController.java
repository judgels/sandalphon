package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.ClientUpsertForm;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.client.createView;
import org.iatoki.judgels.sandalphon.views.html.client.listView;
import org.iatoki.judgels.sandalphon.views.html.client.updateView;
import org.iatoki.judgels.sandalphon.views.html.client.viewView;
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
@Transactional
public final class ClientController extends Controller {

    private static final long PAGE_SIZE = 20;

    private ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;

        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
    }

    public Result index() {
        return list(0, "id", "asc", "");
    }

    private Result showCreate(Form<ClientUpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.create"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.create"), routes.ClientController.create())
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    public Result create() {
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class);

        return showCreate(form);
    }

    @RequireCSRFCheck
    public Result postCreate() {
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            ClientUpsertForm clientUpsertForm = form.get();
            clientService.createClient(clientUpsertForm.name);

            return redirect(routes.ClientController.index());
        }
    }

    public Result view(long clientId) {
        Client client = clientService.findClientById(clientId);
        LazyHtml content = new LazyHtml(viewView.render(client));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("client.client") + " #" + client.getId() + ": " + client.getName(), new InternalLink(Messages.get("commons.update"), routes.ClientController.update(clientId)), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.view"), routes.ClientController.view(clientId))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    private Result showUpdate(Form<ClientUpsertForm> form, Client client) {
        LazyHtml content = new LazyHtml(updateView.render(form, client.getId()));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.client") + " #" + client.getId() + ": " + client.getName(), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.update"), routes.ClientController.update(client.getId()))
        ), c));
        appendTemplateLayout(content);
        return lazyOk(content);
    }

    @AddCSRFToken
    public Result update(long clientId) {
        Client client = clientService.findClientById(clientId);
        ClientUpsertForm clientUpsertForm = new ClientUpsertForm();
        clientUpsertForm.name = client.getName();
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).fill(clientUpsertForm);

        return showUpdate(form, client);
    }

    public Result postUpdate(long clientId) {
        Client client = clientService.findClientById(clientId);
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdate(form, client);
        } else {
            ClientUpsertForm clientUpsertForm = form.get();
            clientService.updateClient(clientId, clientUpsertForm.name);

            return redirect(routes.ClientController.index());
        }
    }

    public Result delete(long clientId) {
        clientService.deleteClient(clientId);

        return redirect(routes.ClientController.index());
    }

    public Result list(long page, String sortBy, String orderBy, String filterString) {
        Page<Client> currentPage = clientService.pageClients(page, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("client.list"), new InternalLink(Messages.get("commons.create"), routes.ClientController.create()), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index())
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
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(routes.ClientController.index().absoluteURL(request())).absoluteURL(request()),
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
