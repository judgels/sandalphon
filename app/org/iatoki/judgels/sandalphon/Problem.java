package org.iatoki.judgels.sandalphon;

public final class Problem {

    private long id;
    private String name;
    private String note;

    protected Problem(long id, String name, String note) {
        this.id = id;
        this.name = name;
        this.note = note;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getNote() {
        return this.note;
    }
}
