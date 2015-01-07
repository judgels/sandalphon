package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem_programming")
public final class ProgrammingProblemModel extends AbstractJudgelsModel {

    public String name;

    public String gradingMethod;

    public String note;

    public ProgrammingProblemModel() {

    }

    public ProgrammingProblemModel(String name, String gradingMethod, String note) {
        this.name = name;
        this.gradingMethod = gradingMethod;
        this.note = note;
    }

    public ProgrammingProblemModel(long id, String jid, String name, String gradingMethod, String note) {
        this(name, gradingMethod, note);
        this.id = id;
        this.jid = jid;
    }
}
