package org.iatoki.judgels.sandalphon;

public interface Problem {

    long getId();

    String getJid();

    String getName();

    String getNote();

    ProblemType getType();
}
