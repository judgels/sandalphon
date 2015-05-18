package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemPartnerConfig;
import play.i18n.Messages;

import java.util.Set;

public final class BundleProblemControllerUtils {
    private BundleProblemControllerUtils() {
        // prevent instantiation
    }

    static void appendTabsLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())));

        if (isAllowedToManageItems(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.bundle.item"), routes.BundleProblemController.jumpToItems(problem.getId())));
        }

        if (isAllowedToSubmit(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.bundle.submission"), routes.BundleProblemController.jumpToSubmissions(problem.getId())));
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

    static BundleProblemPartnerConfig getPartnerConfig(ProblemService problemService, Problem problem) {
        return problemService.findProblemPartnerByProblemJidAndPartnerJid(problem.getJid(), IdentityUtils.getUserJid()).getChildConfig(BundleProblemPartnerConfig.class);
    }

    static boolean isAllowedToManageItems(ProblemService problemService, Problem problem) {
        return ProblemControllerUtils.isAuthorOrAbove(problem) || (ProblemControllerUtils.isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToManageItems());
    }

    static boolean isAllowedToUpdateItemInLanguage(ProblemService problemService, Problem problem) {
        if (!isAllowedToManageItems(problemService, problem)) {
            return false;
        }

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return true;
        }

        if (!ProblemControllerUtils.isPartner(problemService, problem)) {
            return false;
        }

        String language = ProblemControllerUtils.getCurrentStatementLanguage();

        Set<String> allowedLanguages = ProblemControllerUtils.getPartnerConfig(problemService, problem).getAllowedStatementLanguagesToUpdate();

        if (allowedLanguages == null || allowedLanguages.contains(language)) {
            return true;
        }

        String firstLanguage = allowedLanguages.iterator().next();

        ProblemControllerUtils.setCurrentStatementLanguage(firstLanguage);
        return true;
    }

    static boolean isAllowedToSubmit(ProblemService problemService, Problem problem) {
        return ProblemControllerUtils.isAuthorOrAbove(problem) || (ProblemControllerUtils.isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToSubmit());
    }
}
