package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

abstract class ProblemImpl implements Problem {

    private final ProblemModel problemModel;

    ProblemImpl(ProblemModel problemModel) {
        this.problemModel = problemModel;
    }

    @Override
    public long getId() {
        return problemModel.id;
    }

    @Override
    public String getJid() {
        return problemModel.jid;
    }

    @Override
    public String getName() {
        return problemModel.name;
    }

    @Override
    public String getNote() {
        return problemModel.note;
    }
}
