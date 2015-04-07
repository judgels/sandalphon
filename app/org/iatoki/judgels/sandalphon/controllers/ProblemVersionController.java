package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.VersionCommitForm;
import org.iatoki.judgels.sandalphon.views.html.problem.version.listVersionsView;
import org.iatoki.judgels.sandalphon.views.html.problem.version.viewVersionLocalChangesView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProblemVersionController extends Controller {
    private final ProblemService problemService;

    public ProblemVersionController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result listVersionHistory(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToViewVersionHistory(problemService, problem)) {
            List<GitCommit> versions = problemService.getVersions(IdentityUtils.getUserJid(), problem.getJid());
            boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
            boolean isAllowedToRestoreVersionHistory = isClean && ProblemControllerUtils.isAllowedToRestoreVersionHistory(problemService, problem);

            LazyHtml content = new LazyHtml(listVersionsView.render(versions, problem.getId(), isAllowedToRestoreVersionHistory));
            appendSubtabsLayout(content, problem);
            ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.version.history"), routes.ProblemVersionController.listVersionHistory(problem.getId())));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - History");

            ControllerUtils.getInstance().addActivityLog("List version history of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    public Result restoreVersionHistory(long problemId, String hash) {
        Problem problem = problemService.findProblemById(problemId);
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        if (isClean && ProblemControllerUtils.isAllowedToRestoreVersionHistory(problemService, problem)) {
            problemService.restore(problem.getJid(), hash);

            ControllerUtils.getInstance().addActivityLog("Restore version history " + hash + " of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemVersionController.listVersionHistory(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result viewVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

            Form<VersionCommitForm> form = Form.form(VersionCommitForm.class);

            ControllerUtils.getInstance().addActivityLog("View version changes of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return showViewVersionLocalChanges(form, problem, isClean);
        } else {
            return notFound();
        }
    }

    public Result postCommitVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            Form<VersionCommitForm> form = Form.form(VersionCommitForm.class).bindFromRequest();
            if (form.hasErrors() || form.hasGlobalErrors()) {
                boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());
                return showViewVersionLocalChanges(form, problem, isClean);
            }

            VersionCommitForm data = form.get();

            if (problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
                flash("localChangesError", Messages.get("problem.version.local.cantCommit"));
            } else if (!problemService.commitThenMergeUserClone(IdentityUtils.getUserJid(), problem.getJid(), data.title, data.description)) {
                flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
            } else if (!problemService.pushUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
                flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
            } else {
                problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());
            }

            ControllerUtils.getInstance().addActivityLog("Commit version changes of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result updateVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            problemService.fetchUserClone(IdentityUtils.getUserJid(), problem.getJid());

            if (!problemService.updateUserClone(IdentityUtils.getUserJid(), problem.getJid())) {
                flash("localChangesError", Messages.get("problem.version.local.cantMerge"));
            }

            ControllerUtils.getInstance().addActivityLog("Update version changes of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result discardVersionLocalChanges(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isPartnerOrAbove(problemService, problem)) {
            problemService.discardUserClone(IdentityUtils.getUserJid(), problem.getJid());

            ControllerUtils.getInstance().addActivityLog("Discard version changes of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemVersionController.viewVersionLocalChanges(problem.getId()));
        } else {
            return notFound();
        }
    }

    private Result showViewVersionLocalChanges(Form<VersionCommitForm> form, Problem problem, boolean isClean) {
        LazyHtml content = new LazyHtml(viewVersionLocalChangesView.render(form, problem, isClean));
        appendSubtabsLayout(content, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.version.local"), routes.ProblemVersionController.viewVersionLocalChanges(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Versions - Local Changes");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks.add(new InternalLink(Messages.get("problem.version.local"), routes.ProblemVersionController.viewVersionLocalChanges(problem.getId())));

        if (ProblemControllerUtils.isAllowedToViewVersionHistory(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.version.history"), routes.ProblemVersionController.listVersionHistory(problem.getId())));
        }

        content.appendLayout(c -> accessTypesLayout.render(internalLinks.build(), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.version"), routes.ProblemController.jumpToVersions(problem.getId())))
                .add(lastLink)
                .build()
        );
    }

}
