package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.GitCommit;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.VersionCommitForm;
import org.iatoki.judgels.sandalphon.views.html.problem.version.listVersionsView;
import org.iatoki.judgels.sandalphon.views.html.problem.version.viewVersionLocalChangesView;
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
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProblemVersionController extends AbstractJudgelsController {

    private final ProblemService problemService;

    @Inject
    public ProblemVersionController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result listVersionHistory(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAllowedToViewVersionHistory(problemService, problem)) {
            return notFound();
        }

        List<GitCommit> versions = problemService.getVersions(IdentityUtils.getUserJid(), problem.getJid());
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
        boolean isAllowedToRestoreVersionHistory = isClean && ProblemControllerUtils.isAllowedToRestoreVersionHistory(problemService, problem);

        LazyHtml content = new LazyHtml(listVersionsView.render(versions, problem.getId(), isAllowedToRestoreVersionHistory));
        appendSubtabsLayout(content, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.version.history"), routes.ProblemVersionController.listVersionHistory(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - History");

        SandalphonControllerUtils.getInstance().addActivityLog("List version history of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    public Result restoreVersionHistory(long problemId, String hash) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        if (!isClean || !ProblemControllerUtils.isAllowedToRestoreVersionHistory(problemService, problem)) {
            return notFound();
        }

        problemService.restore(problem.getJid(), hash, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        SandalphonControllerUtils.getInstance().addActivityLog("Restore version history " + hash + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemVersionController.listVersionHistory(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result viewVersionLocalChanges(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            return notFound();
        }

        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        Form<VersionCommitForm> versionCommitForm = Form.form(VersionCommitForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("View version changes of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showViewVersionLocalChanges(versionCommitForm, problem, isClean);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCommitVersionLocalChanges(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            return notFound();
        }

        Form<VersionCommitForm> versionCommitForm = Form.form(VersionCommitForm.class).bindFromRequest();
        if (formHasErrors(versionCommitForm)) {
            boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
            return showViewVersionLocalChanges(versionCommitForm, problem, isClean);
        }

        VersionCommitForm versionCommitData = versionCommitForm.get();

        if (problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
            flash("localChangesError", Messages.get("problem.version.local.cantCommit"));
        } else if (!problemService.commitThenMergeUserClone(IdentityUtils.getUserJid(), problem.getJid(), versionCommitData.title, versionCommitData.description, IdentityUtils.getIpAddress())) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        } else if (!problemService.pushUserClone(IdentityUtils.getUserJid(), problem.getJid(), IdentityUtils.getIpAddress())) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        } else {
            try {
                problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                // do nothing
            }
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Commit version changes of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result updateVersionLocalChanges(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            return notFound();
        }

        problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid());

        if (!problemService.updateUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
            flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update version changes of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result discardVersionLocalChanges(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            return notFound();
        }

        try {
            problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());
            SandalphonControllerUtils.getInstance().addActivityLog("Discard version changes of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
        } catch (IOException e) {
            return notFound();
        }
    }

    private Result showViewVersionLocalChanges(Form<VersionCommitForm> versionCommitForm, Problem problem, boolean isClean) {
        LazyHtml content = new LazyHtml(viewVersionLocalChangesView.render(versionCommitForm, problem, isClean));
        appendSubtabsLayout(content, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.version.local"), routes.ProblemVersionController.viewVersionLocalChanges(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - Local Changes");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks.add(new InternalLink(Messages.get("problem.version.local"), routes.ProblemVersionController.viewVersionLocalChanges(problem.getId())));

        if (ProblemControllerUtils.isAllowedToViewVersionHistory(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.version.history"), routes.ProblemVersionController.listVersionHistory(problem.getId())));
        }

        content.appendLayout(c -> subtabLayout.render(internalLinks.build(), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.version"), routes.ProblemController.jumpToVersions(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
