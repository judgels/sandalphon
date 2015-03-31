package org.iatoki.judgels.sandalphon.models.daos.hibernate.programming;


import org.iatoki.judgels.gabriel.commons.models.daos.hibernate.AbstractSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProgrammingSubmissionModel;

public final class ProgrammingSubmissionHibernateDao extends AbstractSubmissionHibernateDao<ProgrammingSubmissionModel> implements ProgrammingSubmissionDao {
    public ProgrammingSubmissionHibernateDao() {
        super(ProgrammingSubmissionModel.class);
    }

    @Override
    public ProgrammingSubmissionModel createSubmissionModel() {
        return new ProgrammingSubmissionModel();
    }
}
