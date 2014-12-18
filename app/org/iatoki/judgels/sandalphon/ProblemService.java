package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.InvalidPageNumberException;

import java.lang.reflect.Field;
import java.util.List;

public interface ProblemService {

    long createProblem(String name, String note);

    Problem getProblem(long id);

    Page<List<String>> pageString(long page, long pageSize, String sortBy, String orderBy, String filterString, List<Field> filters) throws InvalidPageNumberException;

}
