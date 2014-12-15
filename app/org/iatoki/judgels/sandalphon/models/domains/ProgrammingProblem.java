package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.helpers.crud.CrudVisible;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_programmingproblem")
public class ProgrammingProblem extends AbstractJudgelsModel {

    @CrudVisible
    public String name;

    @CrudVisible
    public String note;

    public String statement;

    public ProgrammingProblem() {
        // nothing
    }

    public ProgrammingProblem(String name, String note) {
        this.name = name;
        this.note = note;
    }

    public ProgrammingProblem(long id, String name, String note) {
        this.id = id;
        this.name = name;
        this.note = note;
    }

    @Override
    public String getHumanFriendlyName() {
        return name;
    }
}
