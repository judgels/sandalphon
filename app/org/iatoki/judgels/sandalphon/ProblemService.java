package org.iatoki.judgels.sandalphon;

public interface ProblemService {

    Problem getProblem(long id);

    void updateProblem(long id, String name, String note);

    Problem createProblem(String name, String note);
}
