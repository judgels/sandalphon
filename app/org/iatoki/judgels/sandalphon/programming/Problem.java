package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.GradingType;

public final class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final String gradingType;

    private final String additionalNote;

    public Problem(long id, String jid, String name, String gradingType, String additionalNote) {
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

    public String getGradingType() {
        return gradingType;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }
}
