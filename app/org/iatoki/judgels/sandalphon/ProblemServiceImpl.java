package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

abstract class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;

    @Override
    public final void updateProblem(long id, String name, String note) {
        ProblemModel model = dao.findById(id);
        model.name = name;
        model.note = note;
        dao.edit(model, "TODO", "TODO");
    }

    protected ProblemServiceImpl(ProblemDao dao) {
        this.dao = dao;
    }

    protected final ProblemModel createBaseProblem(String name, String note, String type) {
        ProblemModel model = new ProblemModel(name, note, type);
        dao.persist(model, "TODO", "TODO");
        return model;
    }

    protected final ProblemModel getBaseProblem(long id) {
        return dao.findById(id);
    }
}

