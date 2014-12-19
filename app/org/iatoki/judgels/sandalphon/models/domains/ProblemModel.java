package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem")
public final class ProblemModel extends AbstractJudgelsModel {

    public String name;

    public String note;

    public String type;

    public ProblemModel() {
        // nothing
    }

    public ProblemModel(String name, String note, String type) {
        this.name = name;
        this.note = note;
        this.type = type;
    }

    public ProblemModel(long id, String name, String note, String type) {
        this(name, note, type);
        this.id = id;
    }
}
