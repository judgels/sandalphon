package org.iatoki.judgels.sandalphon.programming;

public final class Grader {

    private final long id;

    private final String jid;

    private final String name;

    private final String secret;

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
