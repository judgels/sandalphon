package org.iatoki.judgels.sandalphon;

public final class Client {

    private long id;

    private String jid;

    private String name;

    private String secret;

    public Client(long id, String jid, String name, String secret) {
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
