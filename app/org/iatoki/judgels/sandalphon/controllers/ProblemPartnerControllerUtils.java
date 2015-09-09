package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.sandalphon.Problem;
import play.i18n.Messages;

final class ProblemPartnerControllerUtils {

    private ProblemPartnerControllerUtils() {
        // prevent instantiation
    }

    static void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.partner"), routes.ProblemController.jumpToPartners(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
