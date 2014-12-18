package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem")
public final class ProblemModel extends AbstractJudgelsModel {

    public String name;

    public String note;

    public ProblemModel() {
        // nothing
    }

    public ProblemModel(String name, String note) {
        this.name = name;
        this.note = note;
    }

    public ProblemModel(long id, String name, String note) {
        this.id = id;
        this.name = name;
        this.note = note;
    }
}
