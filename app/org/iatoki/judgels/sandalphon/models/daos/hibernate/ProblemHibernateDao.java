package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

public final class ProblemHibernateDao extends AbstractJudgelsHibernateDao<ProblemModel> implements ProblemDao {
    public ProblemHibernateDao() {
        super(ProblemModel.class);
    }
}
