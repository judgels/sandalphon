package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.Verdict;

public final class Submission {
    private final long id;
    private final String jid;
    private final String problemJid;
    private final String authorJid;
    private final Verdict verdict;
    private final int score;
    private final String details;

    public Submission(long id, String jid, String problemJid, String authorJid, Verdict verdict, int score, String details) {
        this.id = id;
        this.jid = jid;
        this.problemJid = problemJid;
        this.authorJid = authorJid;
        this.verdict = verdict;
        this.score = score;
        this.details = details;
    }

    public long getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }

    public String getProblemJid() {
        return problemJid;
    }

    public String getAuthorJid() {
        return authorJid;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public int getScore() {
        return score;
    }

    public String getDetails() {
        return details;
    }
}
