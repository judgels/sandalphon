package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

public interface ProblemService {

    Problem findProblemById(long id);

    void updateProblem(long id, String name, String note);

    Page<Problem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString);
}
