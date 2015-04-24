package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ProblemCreateForm;
import org.iatoki.judgels.sandalphon.forms.ProblemUpdateForm;
import org.iatoki.judgels.sandalphon.views.html.problem.createProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.listProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.updateProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewProblemView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProblemController extends BaseController {
    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result index() {
        return listProblems(0, "timeUpdate", "desc", "");
    }

    public Result listProblems(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Problem> problems = problemService.pageProblems(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString, IdentityUtils.getUserJid(), ControllerUtils.getInstance().isAdmin());

        LazyHtml content = new LazyHtml(listProblemsView.render(problems, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.list"), new InternalLink(Messages.get("commons.create"), routes.ProblemController.createProblem()), c));

        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problems");

        ControllerUtils.getInstance().addActivityLog("Open allowed problems <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    public Result createProblem() {
        Form<ProblemCreateForm> form = Form.form(ProblemCreateForm.class);

        ControllerUtils.getInstance().addActivityLog("Try to create problem <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreateProblem(form);
    }

    @RequireCSRFCheck
    public Result postCreateProblem() {
        Form<ProblemCreateForm> form = Form.form(ProblemCreateForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreateProblem(form);
        } else {
            ProblemCreateForm data = form.get();

            if (data.type.equals(ProblemType.PROGRAMMING.name())) {
                ProblemControllerUtils.setJustCreatedProblem(data.name, data.additionalNote, data.initLanguageCode);

                ControllerUtils.getInstance().addActivityLog("Create problem " + data.name + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.ProgrammingProblemController.createProgrammingProblem());
            }

            return internalServerError();
        }
    }

    public Result enterProblem(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Enter problem " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemController.jumpToStatement(problemId));
    }

    public Result jumpToStatement(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to problem statement " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.viewStatement(problemId));
    }

    public Result jumpToVersions(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to problem version " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problemId));
    }

    public Result jumpToPartners(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to problem partner " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemPartnerController.viewPartners(problemId));
    }

    public Result jumpToClients(long problemId) {
        ControllerUtils.getInstance().addActivityLog("Jump to problem client " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemClientController.updateClientProblems(problemId));
    }

    public Result viewProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        LazyHtml content = new LazyHtml(viewProblemView.render(problem));
        appendSubtabs(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                    .add(new InternalLink(Messages.get("problem.view"), routes.ProblemController.viewProblem(problem.getId())))
                    .build()
        );
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View");

        ControllerUtils.getInstance().addActivityLog("View problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    public Result updateProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            ProblemUpdateForm data = new ProblemUpdateForm();
            data.name = problem.getName();
            data.additionalNote = problem.getAdditionalNote();

            Form<ProblemUpdateForm> form = Form.form(ProblemUpdateForm.class).fill(data);

            ControllerUtils.getInstance().addActivityLog("Try to update problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateProblem(form, problem);
        } else {
            return redirect(routes.ProblemController.viewProblem(problem.getId()));
        }
    }

    @RequireCSRFCheck
    public Result postUpdateProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            Form<ProblemUpdateForm> form = Form.form(ProblemUpdateForm.class).bindFromRequest();
            if (form.hasErrors() || form.hasGlobalErrors()) {
                return showUpdateProblem(form, problem);
            } else {
                ProblemUpdateForm data = form.get();
                problemService.updateProblem(problemId, data.name, data.additionalNote);

                ControllerUtils.getInstance().addActivityLog("Update problem " + problem.getName() + ".");

                return redirect(routes.ProblemController.viewProblem(problem.getId()));
            }
        } else {
            return notFound();
        }
    }

    private Result showCreateProblem(Form<ProblemCreateForm> form) {
        LazyHtml content = new LazyHtml(createProblemView.render(form));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.create"), routes.ProblemController.createProblem())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateProblem(Form<ProblemUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateProblemView.render(form, problem));
        appendSubtabs(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.update"), routes.ProblemController.updateProblem(problem.getId())))
                .build()
        );
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabs(LazyHtml content, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewProblem(problem.getId())));

        if (ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.ProblemController.updateProblem(problem.getId())));
        }

        content.appendLayout(c -> accessTypesLayout.render(internalLinks.build(), c));
    }
}
