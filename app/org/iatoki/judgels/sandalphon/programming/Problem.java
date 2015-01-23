package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.GradingType;

public final class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final GradingType gradingType;

    private final String additionalNote;

    public Problem(long id, String jid, String name, GradingType gradingType, String additionalNote) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.gradingType = gradingType;
        this.additionalNote = additionalNote;
    }

    public long getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public GradingType getGradingType() {
        return gradingType;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }
}
