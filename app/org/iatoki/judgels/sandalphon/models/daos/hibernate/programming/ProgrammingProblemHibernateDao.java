package org.iatoki.judgels.sandalphon.models.daos.hibernate.programming;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProgrammingProblemModel;

public final class ProgrammingProblemHibernateDao extends AbstractJudgelsHibernateDao<ProgrammingProblemModel> implements ProgrammingProblemDao {

    public ProgrammingProblemHibernateDao() {
        super(ProgrammingProblemModel.class);
    }
}