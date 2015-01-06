package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem_programming")
public final class ProgrammingProblemModel extends AbstractJudgelsModel {

    public String name;

    public String note;

    public ProgrammingProblemModel() {

    }

    public ProgrammingProblemModel(String name, String note) {
        this.name = name;
        this.note = note;
    }

    public ProgrammingProblemModel(long id, String jid, String name, String note) {
        this(name, note);
        this.id = id;
        this.jid = jid;
    }
}
