package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.play.views.html.layouts.headingLayout;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ProblemCreateForm;
import org.iatoki.judgels.sandalphon.forms.ProblemEditForm;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.createProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.listProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.editProblemView;
import org.iatoki.judgels.sandalphon.views.html.problem.viewProblemView;
import play.data.DynamicForm;
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
@Singleton
@Named
public final class ProblemController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;

    @Inject
    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result index() {
        return listProblems(0, "timeUpdate", "desc", "");
    }

    @Transactional(readOnly = true)
    public Result listProblems(long pageIndex, String sortBy, String orderBy, String filterString) {
        Page<Problem> pageOfProblems = problemService.getPageOfProblems(pageIndex, PAGE_SIZE, sortBy, orderBy, filterString, IdentityUtils.getUserJid(), SandalphonControllerUtils.getInstance().isAdmin());

        LazyHtml content = new LazyHtml(listProblemsView.render(pageOfProblems, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingWithActionLayout.render(Messages.get("problem.list"), new InternalLink(Messages.get("commons.create"), routes.ProblemController.createProblem()), c));

        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problems");

        SandalphonControllerUtils.getInstance().addActivityLog("Open allowed problems <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result createProblem() {
        Form<ProblemCreateForm> problemCreateForm = Form.form(ProblemCreateForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to create problem <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showCreateProblem(problemCreateForm);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreateProblem() {
        Form<ProblemCreateForm> problemCreateForm = Form.form(ProblemCreateForm.class).bindFromRequest();

        if (formHasErrors(problemCreateForm)) {
            return showCreateProblem(problemCreateForm);
        }

        if (problemService.problemExistsBySlug(problemCreateForm.get().slug)) {
            problemCreateForm.reject("slug", Messages.get("error.problem.slugExists"));
        }

        ProblemCreateForm problemCreateData = problemCreateForm.get();
        ProblemControllerUtils.setJustCreatedProblem(problemCreateData.slug, problemCreateData.additionalNote, problemCreateData.initLanguageCode);

        if (problemCreateData.type.equals(ProblemType.PROGRAMMING.name())) {
            SandalphonControllerUtils.getInstance().addActivityLog("Create problem " + problemCreateData.slug + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProgrammingProblemController.createProgrammingProblem());
        } else if (problemCreateData.type.equals(ProblemType.BUNDLE.name())) {
            return redirect(routes.BundleProblemController.createBundleProblem());
        }

        return internalServerError();
    }

    public Result enterProblem(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Enter problem " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemController.jumpToStatement(problemId));
    }

    public Result jumpToStatement(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to problem statement " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemStatementController.viewStatement(problemId));
    }

    public Result jumpToVersions(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to problem version " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problemId));
    }

    public Result jumpToPartners(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to problem partner " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemPartnerController.viewPartners(problemId));
    }

    public Result jumpToClients(long problemId) {
        SandalphonControllerUtils.getInstance().addActivityLog("Jump to problem client " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemClientController.editClientProblems(problemId));
    }

    @Transactional(readOnly = true)
    public Result viewProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        LazyHtml content = new LazyHtml(viewProblemView.render(problem));
        appendSubtabs(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getSlug(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                    .add(new InternalLink(Messages.get("problem.view"), routes.ProblemController.viewProblem(problem.getId())))
                    .build()
        );
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - View");

        SandalphonControllerUtils.getInstance().addActivityLog("View problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            return redirect(routes.ProblemController.viewProblem(problem.getId()));
        }

        ProblemEditForm problemEditData = new ProblemEditForm();
        problemEditData.slug = problem.getSlug();
        problemEditData.additionalNote = problem.getAdditionalNote();

        Form<ProblemEditForm> problemEditForm = Form.form(ProblemEditForm.class).fill(problemEditData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showEditProblem(problemEditForm, problem);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditProblem(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            return notFound();
        }

        Form<ProblemEditForm> problemEditForm = Form.form(ProblemEditForm.class).bindFromRequest();

        if (formHasErrors(problemEditForm)) {
            return showEditProblem(problemEditForm, problem);
        }

        if (!problem.getSlug().equals(problemEditForm.get().slug) && problemService.problemExistsBySlug(problemEditForm.get().slug)) {
            problemEditForm.reject("slug", Messages.get("error.problem.slugExists"));
        }

        ProblemEditForm problemEditData = problemEditForm.get();
        problemService.updateProblem(problem.getJid(), problemEditData.slug, problemEditData.additionalNote, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        SandalphonControllerUtils.getInstance().addActivityLog("Update problem " + problem.getSlug() + ".");

        return redirect(routes.ProblemController.viewProblem(problem.getId()));
    }

    public Result switchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        SandalphonControllerUtils.getInstance().addActivityLog("Switch language to " + languageCode + " of problem " + problemId + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(request().getHeader("Referer"));
    }

    private Result showCreateProblem(Form<ProblemCreateForm> problemCreateForm) {
        LazyHtml content = new LazyHtml(createProblemView.render(problemCreateForm));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.create"), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()),
                new InternalLink(Messages.get("problem.create"), routes.ProblemController.createProblem())
        ));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Create");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showEditProblem(Form<ProblemEditForm> problemEditForm, Problem problem) {
        LazyHtml content = new LazyHtml(editProblemView.render(problemEditForm, problem));
        appendSubtabs(content, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getSlug(), new InternalLink(Messages.get("problem.enter"), routes.ProblemController.enterProblem(problem.getId())), c));
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.update"), routes.ProblemController.editProblem(problem.getId())))
                .build()
        );
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabs(LazyHtml content, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.ProblemController.viewProblem(problem.getId())));

        if (ProblemControllerUtils.isAllowedToUpdateProblem(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.ProblemController.editProblem(problem.getId())));
        }

        content.appendLayout(c -> subtabLayout.render(internalLinks.build(), c));
    }
}
