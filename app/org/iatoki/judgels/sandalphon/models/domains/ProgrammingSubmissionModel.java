package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;
import org.iatoki.judgels.gabriel.blackbox.OverallVerdict;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_submission")
public class ProgrammingSubmissionModel extends AbstractJudgelsModel {

    public String problemJid;

    public OverallVerdict verdict;

    public double score;
}
