package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.heading3Layout;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserInfo;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.ProblemPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.forms.bundle.BundlePartnerUpsertForm;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;
import org.iatoki.judgels.sandalphon.services.JidCacheService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.bundle.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.bundle.partner.updatePartnerView;
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
import java.util.Set;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class BundleProblemPartnerController extends BaseController {

    private final Jophiel jophiel;
    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;

    @Inject
    public BundleProblemPartnerController(Jophiel jophiel, ProblemService problemService, BundleProblemService bundleProblemService) {
        this.jophiel = jophiel;
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result addPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class);
            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class);
            Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class);

            ControllerUtils.getInstance().addActivityLog("Try to add partner of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showAddPartner(usernameForm, problemForm, bundleForm, problem);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postAddPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class).bindFromRequest();
            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
            Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).bindFromRequest();

            if (usernameForm.hasErrors() || usernameForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, bundleForm, problem);
            }

            if (problemForm.hasErrors() || problemForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, bundleForm, problem);
            }

            if (bundleForm.hasErrors() || bundleForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, bundleForm, problem);
            }

            String username = usernameForm.get().username;
            ProblemPartnerUpsertForm problemData = problemForm.get();
            BundlePartnerUpsertForm bundleData = bundleForm.get();

            try {
                String userJid = jophiel.verifyUsername(username);

                if (userJid == null) {
                    usernameForm.reject("username", Messages.get("problem.partner.usernameNotFound"));
                    return showAddPartner(usernameForm, problemForm, bundleForm, problem);
                }

                UserInfo user = jophiel.getUserByUserJid(userJid);
                JidCacheService.getInstance().putDisplayName(user.getJid(), JudgelsUtils.getUserDisplayName(user.getUsername(), user.getName()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

                if (problemService.isProblemPartnerByUserJid(problem.getJid(), userJid)) {
                    usernameForm.reject("username", Messages.get("problem.partner.already"));
                    return showAddPartner(usernameForm, problemForm, bundleForm, problem);
                }

                ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
                      .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
                      .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
                      .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
                      .setAllowedStatementLanguagesToView(splitByComma(problemData.allowedStatementLanguagesToView))
                      .setAllowedStatementLanguagesToUpdate(splitByComma(problemData.allowedStatementLanguagesToUpdate))
                      .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
                      .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
                      .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
                      .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
                      .build();

                BundleProblemPartnerConfig bundleConfig = new BundleProblemPartnerConfig(bundleData.isAllowedToSubmit, bundleData.isAllowedToManageItems);

                problemService.createProblemPartner(problem.getId(), userJid, problemConfig, bundleConfig);

                ControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of problem " + problem.getName() + ".");

                return redirect(routes.ProblemPartnerController.viewPartners(problem.getId()));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            ProblemPartner problemPartner = problemService.findProblemPartnerByProblemPartnerId(partnerId);

            ProblemPartnerConfig problemConfig = problemPartner.getBaseConfig();
            ProblemPartnerUpsertForm problemData = new ProblemPartnerUpsertForm();

            problemData.isAllowedToUpdateProblem = problemConfig.isAllowedToUpdateProblem();
            problemData.isAllowedToUpdateStatement = problemConfig.isAllowedToUpdateStatement();
            problemData.isAllowedToUploadStatementResources = problemConfig.isAllowedToUploadStatementResources();
            problemData.allowedStatementLanguagesToView = combineByComma(problemConfig.getAllowedStatementLanguagesToView());
            problemData.allowedStatementLanguagesToUpdate = combineByComma(problemConfig.getAllowedStatementLanguagesToUpdate());
            problemData.isAllowedToManageStatementLanguages = problemConfig.isAllowedToManageStatementLanguages();
            problemData.isAllowedToViewVersionHistory = problemConfig.isAllowedToViewVersionHistory();
            problemData.isAllowedToRestoreVersionHistory = problemConfig.isAllowedToRestoreVersionHistory();
            problemData.isAllowedToManageProblemClients = problemConfig.isAllowedToManageProblemClients();

            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).fill(problemData);

            BundleProblemPartnerConfig bundleConfig = problemPartner.getChildConfig(BundleProblemPartnerConfig.class);
            BundlePartnerUpsertForm bundleData = new BundlePartnerUpsertForm();

            bundleData.isAllowedToManageItems = bundleConfig.isAllowedToManageItems();

            Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).fill(bundleData);

            ControllerUtils.getInstance().addActivityLog("Try to update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdatePartner(problemForm, bundleForm, problem, problemPartner);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            ProblemPartner problemPartner = problemService.findProblemPartnerByProblemPartnerId(partnerId);

            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
            Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).bindFromRequest();

            if (problemForm.hasErrors() || problemForm.hasGlobalErrors()) {
                return showUpdatePartner(problemForm, bundleForm, problem, problemPartner);
            }

            ProblemPartnerUpsertForm problemData = problemForm.get();

            ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
                  .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
                  .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
                  .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
                  .setAllowedStatementLanguagesToView(splitByComma(problemData.allowedStatementLanguagesToView))
                  .setAllowedStatementLanguagesToUpdate(splitByComma(problemData.allowedStatementLanguagesToUpdate))
                  .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
                  .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
                  .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
                  .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
                  .build();

            BundlePartnerUpsertForm bundleData = bundleForm.get();

            BundleProblemPartnerConfig bundleConfig = new BundleProblemPartnerConfig(bundleData.isAllowedToSubmit, bundleData.isAllowedToManageItems);

            problemService.updateProblemPartner(partnerId, problemConfig, bundleConfig);

            ControllerUtils.getInstance().addActivityLog("Update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getName() + ".");

            return redirect(routes.ProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId()));
        } else {
            return notFound();
        }
    }

    private Result showAddPartner(Form<ProblemPartnerUsernameForm> usernameForm, Form<ProblemPartnerUpsertForm> problemForm, Form<BundlePartnerUpsertForm> bundleForm, Problem problem) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, problemForm, bundleForm, problem, jophiel.getAutoCompleteEndPoint()));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.add"), c));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.add"), routes.BundleProblemPartnerController.addPartner(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Add Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<ProblemPartnerUpsertForm> problemForm, Form<BundlePartnerUpsertForm> bundleForm, Problem problem, ProblemPartner problemPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(problemForm, bundleForm, problem, problemPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.update") + ": " + JidCacheService.getInstance().getDisplayName(problemPartner.getPartnerJid()), c));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.update"), routes.BundleProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Set<String> splitByComma(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Sets.newHashSet(s.split(","));
    }

    private String combineByComma(Set<String> list) {
        if (list == null) {
            return null;
        }
        return Joiner.on(",").join(list);
    }
}
