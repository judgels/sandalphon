package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.JidChildPrefixes;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem")
@JidChildPrefixes({"PROG"})
public final class ProblemModel extends AbstractJudgelsModel {
    public String name;

    @Column(columnDefinition = "TEXT")
    public String additionalNote;
}
