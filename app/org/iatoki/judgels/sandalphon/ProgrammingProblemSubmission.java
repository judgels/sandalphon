package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.gabriel.blackbox.OverallVerdict;

public final class ProgrammingProblemSubmission {
    private final long id;
    private final String jid;
    private final OverallVerdict verdict;
    private final double score;

    public ProgrammingProblemSubmission(long id, String jid, OverallVerdict verdict, double score) {
        this.id = id;
        this.jid = jid;
        this.verdict = verdict;
        this.score = score;
    }

    public long getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }

    public OverallVerdict getVerdict() {
        return verdict;
    }

    public double getScore() {
        return score;
    }
}
