package org.iatoki.judgels.sandalphon;

public interface ProblemServiceProvider {

    ProblemService getByType(String type);
}
