package org.iatoki.judgels.sandalphon.models.daos.impls.programming;


import org.iatoki.judgels.sandalphon.commons.models.daos.hibernate.AbstractSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.ProgrammingSubmissionModel;

public final class ProgrammingSubmissionHibernateDao extends AbstractSubmissionHibernateDao<ProgrammingSubmissionModel> implements ProgrammingSubmissionDao {
    public ProgrammingSubmissionHibernateDao() {
        super(ProgrammingSubmissionModel.class);
    }

    @Override
    public ProgrammingSubmissionModel createSubmissionModel() {
        return new ProgrammingSubmissionModel();
    }
}
