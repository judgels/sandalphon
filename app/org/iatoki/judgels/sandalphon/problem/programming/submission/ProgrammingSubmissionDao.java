package org.iatoki.judgels.sandalphon.problem.programming.submission;

import com.google.inject.ImplementedBy;
import org.iatoki.judgels.sandalphon.models.daos.BaseProgrammingSubmissionDao;

@ImplementedBy(ProgrammingSubmissionHibernateDao.class)
public interface ProgrammingSubmissionDao extends BaseProgrammingSubmissionDao<ProgrammingSubmissionModel> {

}
