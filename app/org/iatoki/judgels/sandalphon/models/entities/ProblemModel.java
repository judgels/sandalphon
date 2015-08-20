package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.play.models.JidChildPrefixes;
import org.iatoki.judgels.play.models.entities.AbstractJudgelsModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem")
@JidChildPrefixes({"PROG", "BUND"})
public final class ProblemModel extends AbstractJudgelsModel {

    public String name;

    @Column(columnDefinition = "TEXT")
    public String additionalNote;
}
