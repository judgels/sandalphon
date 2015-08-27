package org.iatoki.judgels.sandalphon;

import java.util.Date;

public final class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final String authorJid;
    private final String additionalNote;
    private final Date lastUpdateTime;

    private final ProblemType type;

    public Problem(long id, String jid, String name, String authorJid, String additionalNote, Date lastUpdateTime, ProblemType type) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.authorJid = authorJid;
        this.additionalNote = additionalNote;
        this.lastUpdateTime = lastUpdateTime;
        this.type = type;
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

    public String getAdditionalNote() {
        return additionalNote;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public ProblemType getType() {
        return type;
    }
}
