package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.commons.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.Problem;
import play.i18n.Messages;

public final class ProgrammingProblemControllerUtils {
    private ProgrammingProblemControllerUtils() {
        // prevent instantiation
    }

    public static void appendTabsLayout(LazyHtml content, Problem problem) {
        content.appendLayout(c -> tabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.statement"), routes.ProblemController.jumpToStatement(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())),
                new InternalLink(Messages.get("problem.programming.submission"), routes.ProgrammingProblemController.jumpToSubmissions(problem.getId())),
                new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId()))
        ), c));

        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.update"), routes.ProblemController.updateProblem(problem.getId())), c));
    }
}
