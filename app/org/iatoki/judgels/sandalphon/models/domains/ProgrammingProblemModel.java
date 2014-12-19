package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem_programming")
public final class ProgrammingProblemModel extends AbstractModel {
    @Id
    public long id;

    public String problemJid;

    public String difficulty;

    public ProgrammingProblemModel(String problemJid, String difficulty) {
        this.problemJid = problemJid;
        this.difficulty = difficulty;
    }

    public ProgrammingProblemModel(long id, String problemJid, String difficulty) {
        this(problemJid, difficulty);
        this.id = id;
    }
}
