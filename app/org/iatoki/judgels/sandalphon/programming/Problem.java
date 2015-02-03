package org.iatoki.judgels.sandalphon.programming;

public final class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final String gradingType;
    private final long timeUpdate;

    private final String additionalNote;

    public Problem(long id, String jid, String name, String gradingType, long timeUpdate, String additionalNote) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.gradingType = gradingType;
        this.timeUpdate = timeUpdate;
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

    public long getTimeUpdate() {
        return timeUpdate;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }
}
