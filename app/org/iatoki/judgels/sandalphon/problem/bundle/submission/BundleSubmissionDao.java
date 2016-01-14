package org.iatoki.judgels.sandalphon.problem.bundle.submission;

import com.google.inject.ImplementedBy;
import org.iatoki.judgels.sandalphon.models.daos.BaseBundleSubmissionDao;

@ImplementedBy(BundleSubmissionHibernateDao.class)
public interface BundleSubmissionDao extends BaseBundleSubmissionDao<BundleSubmissionModel> {

}
