package org.iatoki.judgels.sandalphon;

public abstract class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final String note;

    public Problem(long id, String jid, String name, String note) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.note = note;
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

    public String getNote() {
        return note;
    }
}
