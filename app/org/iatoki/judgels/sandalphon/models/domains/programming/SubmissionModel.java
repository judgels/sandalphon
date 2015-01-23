package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.gabriel.Verdict;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_submission")
public final class SubmissionModel extends org.iatoki.judgels.commons.models.domains.SubmissionModel {

    public SubmissionModel() {

    }

    public SubmissionModel(long id, String jid, String problemJid, Verdict verdict, int score, String message, char[] details) {
        this.id = id;
        this.jid = jid;
        this.problemJid = problemJid;
        this.verdict = verdict;
        this.score = score;
        this.message = message;
        this.details = details;
    }
}
