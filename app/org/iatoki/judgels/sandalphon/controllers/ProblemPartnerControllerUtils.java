package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.sandalphon.Problem;
import play.i18n.Messages;

public final class ProblemPartnerControllerUtils {

    private ProblemPartnerControllerUtils() {
        // prevent instantiation
    }

    static void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.partner"), routes.ProblemController.jumpToPartners(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
