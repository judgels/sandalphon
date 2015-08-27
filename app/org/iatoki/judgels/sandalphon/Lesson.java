package org.iatoki.judgels.sandalphon;

import java.util.Date;

public final class Lesson {

    private final long id;
    private final String jid;
    private final String name;
    private final String authorJid;
    private final String additionalNote;
    private final Date lastUpdateTime;

    public Lesson(long id, String jid, String name, String authorJid, String additionalNote, Date lastUpdateTime) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.authorJid = authorJid;
        this.additionalNote = additionalNote;
        this.lastUpdateTime = lastUpdateTime;
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
}
