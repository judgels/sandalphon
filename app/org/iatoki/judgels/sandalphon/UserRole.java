package org.iatoki.judgels.sandalphon;

import java.util.List;

public final class UserRole {

    private long id;

    private String userJid;

    private String username;

    private List<String> roles;

    public UserRole(long id, String userJid, String username, List<String> roles) {
        this.id = id;
        this.userJid = userJid;
        this.username = username;
        this.roles = roles;
    }

    public long getId() {
        return id;
    }

    public String getUserJid() {
        return userJid;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
