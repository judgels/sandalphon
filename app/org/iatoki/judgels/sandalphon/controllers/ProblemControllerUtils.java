package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.ProblemType;

public final class ProblemControllerUtils {
    private static final ProblemControllerUtils INSTANCE = new ProblemControllerUtils();

    public void appendTabsLayout(LazyHtml content, Problem problem) {
        if (problem.getType() == ProblemType.PROGRAMMING) {
            ProgrammingProblemControllerUtils.getInstance().appendTabsLayout(content, problem);
        }
    }

    static ProblemControllerUtils getInstance() {
        return INSTANCE;
    }
}
