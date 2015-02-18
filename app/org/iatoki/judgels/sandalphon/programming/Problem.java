package org.iatoki.judgels.sandalphon.programming;

import java.util.Date;

public final class Problem {
    private final long id;
    private final String jid;
    private final String name;
    private final String authorJid;
    private final String gradingEngine;
    private final Date lastUpdate;

    private final String additionalNote;

    public Problem(long id, String jid, String name, String authorJid, String gradingEngine, Date lastUpdate, String additionalNote) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.authorJid = authorJid;
        this.gradingEngine = gradingEngine;
        this.lastUpdate = lastUpdate;
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

    public String getAuthorJid() {
        return authorJid;
    }

    public String getGradingEngine() {
        return gradingEngine;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }
}
