package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.headingLayout;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.Authorized;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ClientUpsertForm;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.views.html.client.createView;
import org.iatoki.judgels.sandalphon.views.html.client.listView;
import org.iatoki.judgels.sandalphon.views.html.client.updateView;
import org.iatoki.judgels.sandalphon.views.html.client.viewView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Authorized(value = "admin")
@Singleton
@Named
public final class ClientController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final ClientService clientService;

    @Inject
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Transactional(readOnly = true)
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Transactional(readOnly = true)
    public Result list(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Client> currentPage = clientService.pageClients(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("client.list"), new InternalLink(Messages.get("commons.create"), routes.ClientController.create()), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index())
        ));

        ControllerUtils.getInstance().appendTemplateLayout(content, "Clients - List");

        ControllerUtils.getInstance().addActivityLog("List all clients <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result create() {
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class);

        ControllerUtils.getInstance().addActivityLog("Try to create client <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreate(form);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreate() {
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            ClientUpsertForm clientUpsertForm = form.get();
            clientService.createClient(clientUpsertForm.name);

            ControllerUtils.getInstance().addActivityLog("Create client " + clientUpsertForm.name + ".");

            return redirect(routes.ClientController.index());
        }
    }

    @Transactional(readOnly = true)
    public Result view(long clientId) throws ClientNotFoundException {
        Client client = clientService.findClientById(clientId);
        LazyHtml content = new LazyHtml(viewView.render(client));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("client.client") + " #" + client.getId() + ": " + client.getName(), new InternalLink(Messages.get("commons.update"), routes.ClientController.update(clientId)), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.view"), routes.ClientController.view(clientId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Client - View");

        ControllerUtils.getInstance().addActivityLog("View client " + client.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result update(long clientId) throws ClientNotFoundException {
        Client client = clientService.findClientById(clientId);
        ClientUpsertForm clientUpsertForm = new ClientUpsertForm();
        clientUpsertForm.name = client.getName();
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).fill(clientUpsertForm);

        ControllerUtils.getInstance().addActivityLog("Try to update client " + client.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdate(form, client);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdate(long clientId) throws ClientNotFoundException {
        Client client = clientService.findClientById(clientId);
        Form<ClientUpsertForm> form = Form.form(ClientUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdate(form, client);
        } else {
            ClientUpsertForm clientUpsertForm = form.get();
            clientService.updateClient(clientId, clientUpsertForm.name);

            ControllerUtils.getInstance().addActivityLog("Update client " + client.getName() + ".");

            return redirect(routes.ClientController.index());
        }
    }

    @Transactional
    public Result delete(long clientId) throws ClientNotFoundException {
        Client client = clientService.findClientById(clientId);
        clientService.deleteClient(client.getId());

        ControllerUtils.getInstance().addActivityLog("Delete client " + client.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ClientController.index());
    }

    private Result showCreate(Form<ClientUpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.create"), routes.ClientController.create())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Client - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdate(Form<ClientUpsertForm> form, Client client) {
        LazyHtml content = new LazyHtml(updateView.render(form, client.getId()));
        content.appendLayout(c -> headingLayout.render(Messages.get("client.client") + " #" + client.getId() + ": " + client.getName(), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("client.clients"), routes.ClientController.index()),
                new InternalLink(Messages.get("client.update"), routes.ClientController.update(client.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Client - Update");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
