package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.commons.models.JidPrefix;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_problem_programming")
@JidPrefix("PROG")
public final class ProgrammingProblemModel extends AbstractJudgelsModel {

    public String gradingEngine;

    @Column(columnDefinition = "TEXT")
    public String additionalNote;

    @Column(columnDefinition = "TEXT")
    public String languageRestriction;
}
