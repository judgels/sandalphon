package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.play.AttributeNotAvailableException;

import java.util.Date;

public final class Lesson {

    private final long id;
    private final String jid;
    private final String name;
    private final String authorJid;
    private final String additionalNote;
    private final Date lastUpdateTime;

    public Lesson(String jid) {
        this.id = -1;
        this.jid = jid;
        this.name = null;
        this.authorJid = null;
        this.additionalNote = null;
        this.lastUpdateTime = null;
    }

    public Lesson(long id, String jid, String name, String authorJid, String additionalNote, Date lastUpdateTime) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.authorJid = authorJid;
        this.additionalNote = additionalNote;
        this.lastUpdateTime = lastUpdateTime;
    }

    public long getId() {
        if (id == -1) {
            throw new AttributeNotAvailableException("id");
        }
        return id;
    }

    public String getJid() {
        return jid;
    }

    public String getName() {
        if (name == null) {
            throw new AttributeNotAvailableException("name");
        }
        return name;
    }

    public String getAuthorJid() {
        if (authorJid == null) {
            throw new AttributeNotAvailableException("authorJid");
        }
        return authorJid;
    }

    public String getAdditionalNote() {
        if (additionalNote == null) {
            throw new AttributeNotAvailableException("additionalNote");
        }
        return additionalNote;
    }

    public Date getLastUpdateTime() {
        if (lastUpdateTime == null) {
            throw new AttributeNotAvailableException("lastUpdateTime");
        }
        return lastUpdateTime;
    }
}
