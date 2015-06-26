package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.programming.Grader;
import org.iatoki.judgels.sandalphon.programming.GraderNotFoundException;
import org.iatoki.judgels.sandalphon.services.GraderService;
import org.iatoki.judgels.sandalphon.forms.GraderUpsertForm;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.Authorized;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.programming.grader.createView;
import org.iatoki.judgels.sandalphon.views.html.programming.grader.listView;
import org.iatoki.judgels.sandalphon.views.html.programming.grader.updateView;
import org.iatoki.judgels.sandalphon.views.html.programming.grader.viewView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Authorized(value = {"admin"})
public final class GraderController extends BaseController {

    private static final long PAGE_SIZE = 20;

    private final GraderService graderService;

    public GraderController(GraderService graderService) {
        this.graderService = graderService;
    }

    @Transactional(readOnly = true)
    public Result index() {
        return list(0, "id", "asc", "");
    }

    @Transactional(readOnly = true)
    public Result list(long pageIndex, String orderBy, String orderDir, String filterString) {
        Page<Grader> currentPage = graderService.pageGraders(pageIndex, PAGE_SIZE, orderBy, orderDir, filterString);

        LazyHtml content = new LazyHtml(listView.render(currentPage, orderBy, orderDir, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("grader.list"), new InternalLink(Messages.get("commons.create"), routes.GraderController.create()), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index())
        ));

        ControllerUtils.getInstance().appendTemplateLayout(content, "Graders - List");

        ControllerUtils.getInstance().addActivityLog("Open graders <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result create() {
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class);

        ControllerUtils.getInstance().addActivityLog("Try to create grader <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreate(form);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreate() {
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreate(form);
        } else {
            GraderUpsertForm graderUpsertForm = form.get();
            graderService.createGrader(graderUpsertForm.name);

            ControllerUtils.getInstance().addActivityLog("Create grader " + graderUpsertForm.name + ".");

            return redirect(routes.GraderController.index());
        }
    }

    @Transactional(readOnly = true)
    public Result view(long graderId) throws GraderNotFoundException {
        Grader grader = graderService.findGraderById(graderId);
        LazyHtml content = new LazyHtml(viewView.render(grader));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("grader.grader") + " #" + grader.getId() + ": " + grader.getName(), new InternalLink(Messages.get("commons.update"), routes.GraderController.update(graderId)), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.view"), routes.GraderController.view(graderId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Grader - View");

        ControllerUtils.getInstance().addActivityLog("View grader " + grader.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result update(long graderId) throws GraderNotFoundException {
        Grader grader = graderService.findGraderById(graderId);
        GraderUpsertForm graderUpsertForm = new GraderUpsertForm();
        graderUpsertForm.name = grader.getName();
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).fill(graderUpsertForm);

        ControllerUtils.getInstance().addActivityLog("Try to update grader " + grader.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdate(form, grader);
    }

    @Transactional
    public Result postUpdate(long graderId) throws GraderNotFoundException {
        Grader grader = graderService.findGraderById(graderId);
        Form<GraderUpsertForm> form = Form.form(GraderUpsertForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showUpdate(form, grader);
        } else {
            GraderUpsertForm graderUpsertForm = form.get();
            graderService.updateGrader(graderId, graderUpsertForm.name);

            ControllerUtils.getInstance().addActivityLog("Update grader " + grader.getName() + ".");

            return redirect(routes.GraderController.index());
        }
    }

    private Result showCreate(Form<GraderUpsertForm> form) {
        LazyHtml content = new LazyHtml(createView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("grader.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.create"), routes.GraderController.create())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Grader - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdate(Form<GraderUpsertForm> form, Grader grader) {
        LazyHtml content = new LazyHtml(updateView.render(form, grader.getId()));
        content.appendLayout(c -> headingLayout.render(Messages.get("grader.grader") + " #" + grader.getId() + ": " + grader.getName(), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()),
                new InternalLink(Messages.get("grader.update"), routes.GraderController.update(grader.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Grader - Update");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
