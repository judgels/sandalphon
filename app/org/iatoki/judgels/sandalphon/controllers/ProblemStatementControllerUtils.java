package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import play.i18n.Messages;

public final class ProblemStatementControllerUtils {

    private ProblemStatementControllerUtils() {
        // prevent instantiation
    }

    static void appendSubtabsLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.ProblemStatementController.viewStatement(problem.getId())));

        if (ProblemControllerUtils.isAllowedToUpdateStatement(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.ProblemStatementController.updateStatement(problem.getId())));
        }

        internalLinks.add(new InternalLink(Messages.get("problem.statement.media"), routes.ProblemStatementController.listStatementMediaFiles(problem.getId())));

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            internalLinks.add(new InternalLink(Messages.get("problem.statement.language"), routes.ProblemStatementController.listStatementLanguages(problem.getId())));
        }

        content.appendLayout(c -> accessTypesLayout.render(internalLinks.build(), c));
    }

    static void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                        .add(new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())))
                        .add(lastLink)
                        .build()
        );
    }
}
