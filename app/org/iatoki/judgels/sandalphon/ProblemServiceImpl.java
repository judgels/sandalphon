package org.iatoki.judgels.sandalphon;


import org.iatoki.judgels.commons.InvalidPageNumberException;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.lang.reflect.Field;
import java.util.List;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;

    public ProblemServiceImpl(ProblemDao dao) {
        this.dao = dao;
    }

    @Override
    public long createProblem(String name, String note) {
        ProblemModel problem = new ProblemModel(name, note);
        return dao.persist(problem, "sdf", null);
    }

    @Override
    public Problem getProblem(long id) {
        ProblemModel problem = dao.findById(id);
        return new Problem(problem.id, problem.name, problem.note);
    }

    @Override
    public Page<List<String>> pageString(long page, long pageSize, String sortBy, String orderBy, String filterString, List<Field> filters) throws InvalidPageNumberException {
        return dao.pageString(page, 20, sortBy, orderBy, filterString, filters);
    }
}
