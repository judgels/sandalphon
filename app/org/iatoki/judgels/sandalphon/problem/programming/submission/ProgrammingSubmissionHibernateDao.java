package org.iatoki.judgels.sandalphon.problem.programming.submission;

import org.iatoki.judgels.sandalphon.models.daos.hibernate.AbstractProgrammingSubmissionHibernateDao;

import javax.inject.Singleton;

@Singleton
public final class ProgrammingSubmissionHibernateDao extends AbstractProgrammingSubmissionHibernateDao<ProgrammingSubmissionModel> implements ProgrammingSubmissionDao {

    public ProgrammingSubmissionHibernateDao() {
        super(ProgrammingSubmissionModel.class);
    }

    @Override
    public ProgrammingSubmissionModel createSubmissionModel() {
        return new ProgrammingSubmissionModel();
    }
}
