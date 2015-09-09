package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.PublicUser;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.heading3Layout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.ProblemPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.ProgrammingProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProgrammingProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.forms.ProgrammingPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.partner.updatePartnerView;
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
import java.io.IOException;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProgrammingProblemPartnerController extends AbstractJudgelsController {

    private final Jophiel jophiel;
    private final ProblemService problemService;

    @Inject
    public ProgrammingProblemPartnerController(Jophiel jophiel, ProblemService problemService) {
        this.jophiel = jophiel;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result addPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class);
        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class);
        Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to add partner of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showAddPartner(usernameForm, problemForm, programmingForm, problem);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postAddPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class).bindFromRequest();
        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
        Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(usernameForm) || formHasErrors(problemForm) || formHasErrors(programmingForm)) {
            return showAddPartner(usernameForm, problemForm, programmingForm, problem);
        }

        String username = usernameForm.get().username;
        ProblemPartnerUpsertForm problemData = problemForm.get();
        ProgrammingPartnerUpsertForm programmingData = programmingForm.get();

        String userJid;
        try {
            userJid = jophiel.verifyUsername(username);
        } catch (IOException e) {
            return notFound();
        }

        if (userJid == null) {
            usernameForm.reject("username", Messages.get("problem.partner.usernameNotFound"));
            return showAddPartner(usernameForm, problemForm, programmingForm, problem);
        }

        PublicUser publicUser;
        try {
            publicUser = jophiel.getPublicUserByJid(userJid);
        } catch (IOException e) {
            return notFound();
        }

        JidCacheServiceImpl.getInstance().putDisplayName(publicUser.getJid(), JudgelsPlayUtils.getUserDisplayName(publicUser.getUsername()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        if (problemService.isUserPartnerForProblem(problem.getJid(), userJid)) {
            usernameForm.reject("username", Messages.get("problem.partner.already"));
            return showAddPartner(usernameForm, problemForm, programmingForm, problem);
        }

        ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
              .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
              .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
              .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
              .setAllowedStatementLanguagesToView(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToView))
              .setAllowedStatementLanguagesToUpdate(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToUpdate))
              .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
              .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
              .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
              .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
              .build();

        ProgrammingProblemPartnerConfig programmingConfig = new ProgrammingProblemPartnerConfigBuilder()
              .setIsAllowedToSubmit(programmingData.isAllowedToSubmit)
              .setIsAllowedToManageGrading(programmingData.isAllowedToManageGrading)
              .build();

        problemService.createProblemPartner(problem.getId(), userJid, problemConfig, programmingConfig);

        SandalphonControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of problem " + problem.getSlug() + ".");

        return redirect(routes.ProblemPartnerController.viewPartners(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        ProblemPartner problemPartner = problemService.findProblemPartnerById(partnerId);

        ProblemPartnerConfig problemConfig = problemPartner.getBaseConfig();
        ProblemPartnerUpsertForm problemData = new ProblemPartnerUpsertForm();

        problemData.isAllowedToUpdateProblem = problemConfig.isAllowedToUpdateProblem();
        problemData.isAllowedToUpdateStatement = problemConfig.isAllowedToUpdateStatement();
        problemData.isAllowedToUploadStatementResources = problemConfig.isAllowedToUploadStatementResources();
        problemData.allowedStatementLanguagesToView = PartnerControllerUtils.combineByComma(problemConfig.getAllowedStatementLanguagesToView());
        problemData.allowedStatementLanguagesToUpdate = PartnerControllerUtils.combineByComma(problemConfig.getAllowedStatementLanguagesToUpdate());
        problemData.isAllowedToManageStatementLanguages = problemConfig.isAllowedToManageStatementLanguages();
        problemData.isAllowedToViewVersionHistory = problemConfig.isAllowedToViewVersionHistory();
        problemData.isAllowedToRestoreVersionHistory = problemConfig.isAllowedToRestoreVersionHistory();
        problemData.isAllowedToManageProblemClients = problemConfig.isAllowedToManageProblemClients();

        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).fill(problemData);

        ProgrammingProblemPartnerConfig programmingConfig = problemPartner.getChildConfig(ProgrammingProblemPartnerConfig.class);
        ProgrammingPartnerUpsertForm programmingData = new ProgrammingPartnerUpsertForm();

        programmingData.isAllowedToSubmit = programmingConfig.isAllowedToSubmit();
        programmingData.isAllowedToManageGrading = programmingConfig.isAllowedToManageGrading();

        Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).fill(programmingData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdatePartner(problemForm, programmingForm, problem, problemPartner);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        ProblemPartner problemPartner = problemService.findProblemPartnerById(partnerId);

        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
        Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(problemForm) || formHasErrors(programmingForm)) {
            return showUpdatePartner(problemForm, programmingForm, problem, problemPartner);
        }

        ProblemPartnerUpsertForm problemData = problemForm.get();

        ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
                .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
                .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
                .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
                .setAllowedStatementLanguagesToView(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToView))
                .setAllowedStatementLanguagesToUpdate(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToUpdate))
                .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
                .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
                .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
                .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
                .build();

        ProgrammingPartnerUpsertForm programmingData = programmingForm.get();

        ProgrammingProblemPartnerConfig programmingConfig = new ProgrammingProblemPartnerConfigBuilder()
                .setIsAllowedToSubmit(programmingData.isAllowedToSubmit)
                .setIsAllowedToManageGrading(programmingData.isAllowedToManageGrading)
                .build();


        problemService.updateProblemPartner(partnerId, problemConfig, programmingConfig);

        SandalphonControllerUtils.getInstance().addActivityLog("Update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getSlug() + ".");

        return redirect(routes.ProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId()));
    }

    private Result showAddPartner(Form<ProblemPartnerUsernameForm> usernameForm, Form<ProblemPartnerUpsertForm> problemForm, Form<ProgrammingPartnerUpsertForm> programmingForm, Problem problem) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, problemForm, programmingForm, problem, jophiel.getAutoCompleteEndPoint()));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.add"), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.add"), routes.ProgrammingProblemPartnerController.addPartner(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Add Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<ProblemPartnerUpsertForm> problemForm, Form<ProgrammingPartnerUpsertForm> programmingForm, Problem problem, ProblemPartner problemPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(problemForm, programmingForm, problem, problemPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.update") + ": " + JidCacheServiceImpl.getInstance().getDisplayName(problemPartner.getPartnerJid()), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.update"), routes.ProgrammingProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }
}
