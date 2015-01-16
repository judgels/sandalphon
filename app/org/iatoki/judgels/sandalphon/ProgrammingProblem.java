package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.gabriel.GradingExecutor;

public final class ProgrammingProblem extends Problem {

    private final GradingExecutor gradingMethod;

    public ProgrammingProblem(long id, String jid, String name, GradingExecutor gradingMethod, String note) {
        super(id, jid, name, note);
        this.gradingMethod = gradingMethod;
    }

    public GradingExecutor getGradingMethod() {
        return gradingMethod;
    }
}
