package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.gabriel.Verdict;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_submission")
public final class SubmissionModel extends org.iatoki.judgels.commons.models.domains.SubmissionModel {

    public SubmissionModel() {

    }

    public SubmissionModel(long id, String jid, String problemJid, String verdictCode, String verdictName, int score, String details) {
        this.id = id;
        this.jid = jid;
        this.problemJid = problemJid;
        this.verdictCode = verdictCode;
        this.verdictName = verdictName;
        this.score = score;
        this.details = details;
    }
}
