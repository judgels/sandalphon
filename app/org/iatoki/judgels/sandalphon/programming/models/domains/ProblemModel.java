package org.iatoki.judgels.sandalphon.programming.models.domains;

import org.iatoki.judgels.commons.models.JidPrefix;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_programming_problem")
@JidPrefix("PROG")
public final class ProblemModel extends AbstractJudgelsModel {
    public String name;

    public String gradingEngine;

    public String additionalNote;

    @Column(columnDefinition = "TEXT")
    public String languageRestriction;

    public ProblemModel() {

    }

    public ProblemModel(String name, String gradingEngine, String additionalNote, String languageRestriction) {
        this.name = name;
        this.gradingEngine = gradingEngine;
        this.additionalNote = additionalNote;
        this.languageRestriction = languageRestriction;
    }

    public ProblemModel(long id, String jid, String name, String gradingEngine, String additionalNote, String languageRestriction) {
        this(name, gradingEngine, additionalNote, languageRestriction);
        this.id = id;
        this.jid = jid;
    }
}
