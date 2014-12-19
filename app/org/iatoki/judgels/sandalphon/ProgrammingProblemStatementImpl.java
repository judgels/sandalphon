package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

final class ProgrammingProblemStatementImpl extends ProblemImpl implements ProgrammingProblemStatement {

    private final String statement;

    ProgrammingProblemStatementImpl(ProblemModel baseProblemModel, String statement) {
        super(baseProblemModel);
        this.statement = statement;
    }

    @Override
    public String getStatement() {
        return statement;
    }
}
