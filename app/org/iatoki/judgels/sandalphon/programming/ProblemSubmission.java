package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.sandalphon.commons.Submission;
import org.iatoki.judgels.gabriel.Verdict;

public final class ProblemSubmission implements Submission {
    private final long id;
    private final String jid;
    private final String problemJid;
    private final String authorJid;
    private final String gradingLanguage;
    private final String gradingEngine;
    private final long timestamp;
    private final Verdict verdict;
    private final String message;
    private final int score;
    private final String details;

    public ProblemSubmission(long id, String jid, String problemJid, String authorJid, String gradingLanguage, String gradingEngine, long timestamp, Verdict verdict, String message, int score, String details) {
        this.id = id;
        this.jid = jid;
        this.problemJid = problemJid;
        this.authorJid = authorJid;
        this.gradingLanguage = gradingLanguage;
        this.gradingEngine = gradingEngine;
        this.timestamp = timestamp;
        this.verdict = verdict;
        this.message = message;
        this.score = score;
        this.details = details;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getJid() {
        return jid;
    }

    @Override
    public String getProblemJid() {
        return problemJid;
    }

    @Override
    public String getContestJid() {
        return null;
    }

    @Override
    public String getAuthorJid() {
        return authorJid;
    }

    @Override
    public String getGradingLanguage() {
        return gradingLanguage;
    }

    @Override
    public String getGradingEngine() {
        return gradingEngine;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public Verdict getVerdict() {
        return verdict;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public String getDetails() {
        return details;
    }
}
