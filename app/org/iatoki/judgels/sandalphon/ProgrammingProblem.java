package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.gabriel.GradingMethod;

public final class ProgrammingProblem extends Problem {

    private final GradingMethod gradingMethod;

    public ProgrammingProblem(long id, String jid, String name, GradingMethod gradingMethod, String note) {
        super(id, jid, name, note);
        this.gradingMethod = gradingMethod;
    }

    public GradingMethod getGradingMethod() {
        return gradingMethod;
    }
}
