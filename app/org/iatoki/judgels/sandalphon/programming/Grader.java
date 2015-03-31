package org.iatoki.judgels.sandalphon.programming;

public final class Grader {

    private long id;

    private String jid;

    private String name;

    private String secret;

    public Grader(long id, String jid, String name, String secret) {
        this.id = id;
        this.jid = jid;
        this.name = name;
        this.secret = secret;
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

    public String getSecret() {
        return secret;
    }
}
