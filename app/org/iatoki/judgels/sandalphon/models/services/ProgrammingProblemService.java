package org.iatoki.judgels.sandalphon.models.services;

import org.iatoki.judgels.commons.models.daos.DaoFactory;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblem;

public final class ProgrammingProblemService {

    private ProgrammingProblemService() {
        // prevent instantiation
    }

    public static void updateStatement(long id, String newStatement, String user, String ipAddress) {
        ProgrammingProblemDao dao = DaoFactory.getInstance().getDao(ProgrammingProblemDao.class);
        ProgrammingProblem problem = dao.findById(id);
        problem.statement = newStatement;

        dao.persist(problem, user, ipAddress);
    }
}
