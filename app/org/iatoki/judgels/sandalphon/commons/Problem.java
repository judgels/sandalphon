package org.iatoki.judgels.sandalphon.commons;

import java.util.Date;

public class Problem {
    private final long id;
    private final String jid;
    private final String name;
    private final String authorJid;
    private final Date lastUpdate;

    private final ProblemType type;

    public Problem(String jid, ProblemType type) {
        this.id = -1;
        this.jid = jid;
        this.name = null;
        this.authorJid = null;
        this.lastUpdate = null;
        this.type = type;
    }

    public Problem(long id, String jid, String name, String authorJid, Date lastUpdate, ProblemType type) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.authorJid = authorJid;
        this.lastUpdate = lastUpdate;
        this.type = type;
    }

    public final long getId() {
        return id;
    }

    public final String getJid() {
        return jid;
    }

    public final String getName() {
        return name;
    }

    public final String getAuthorJid() {
        return authorJid;
    }

    public final Date getLastUpdate() {
        return lastUpdate;
    }

    public final ProblemType getType() {
        return type;
    }
}
