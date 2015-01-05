package org.iatoki.judgels.sandalphon;

public class Problem {

    private final long id;
    private final String jid;
    private final String name;
    private final String note;
    private final ProblemType type;

    public Problem(long id, String jid, String name, String note, ProblemType type) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.note = note;
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

    public String getNote() {
        return note;
    }

    public ProblemType getType() {
        return type;
    }
}
