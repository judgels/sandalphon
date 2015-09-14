package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.ProgrammingProblemPartnerConfig;
import play.i18n.Messages;

public final class ProgrammingProblemControllerUtils {

    private ProgrammingProblemControllerUtils() {
        // prevent instantiation
    }

    public static void appendTabsLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())));

        if (isAllowedToManageGrading(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())));
        }

        if (isAllowedToSubmit(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problem.getId())));
        }

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.partner"), routes.ProblemController.jumpToPartners(problem.getId())));
        }

        internalLinks.add(new InternalLink(Messages.get("problem.version"), routes.ProblemController.jumpToVersions(problem.getId())));

        if (ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId())));
        }

        content.appendLayout(c -> tabLayout.render(internalLinks.build(), c));
    }

    public static ProgrammingProblemPartnerConfig getPartnerConfig(ProblemService problemService, Problem problem) {
        return problemService.findProblemPartnerByProblemJidAndPartnerJid(problem.getJid(), IdentityUtils.getUserJid()).getChildConfig(ProgrammingProblemPartnerConfig.class);
    }

    public static boolean isAllowedToManageGrading(ProblemService problemService, Problem problem) {
        return ProblemControllerUtils.isAuthorOrAbove(problem) || (ProblemControllerUtils.isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToManageGrading());
    }

    public static boolean isAllowedToSubmit(ProblemService problemService, Problem problem) {
        return ProblemControllerUtils.isAuthorOrAbove(problem) || (ProblemControllerUtils.isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToSubmit());
    }
}
