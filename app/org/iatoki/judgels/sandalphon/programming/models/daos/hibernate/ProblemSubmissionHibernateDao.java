package org.iatoki.judgels.sandalphon.programming.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemSubmissionModel;

public final class ProblemSubmissionHibernateDao extends AbstractJudgelsHibernateDao<ProblemSubmissionModel> implements ProblemSubmissionDao {

    public ProblemSubmissionHibernateDao() {
        super(ProblemSubmissionModel.class);
    }
}
