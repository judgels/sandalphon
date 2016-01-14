package org.iatoki.judgels.sandalphon.problem.bundle.grading;

import com.google.inject.ImplementedBy;
import org.iatoki.judgels.sandalphon.models.daos.BaseBundleGradingDao;

@ImplementedBy(BundleGradingHibernateDao.class)
public interface BundleGradingDao extends BaseBundleGradingDao<BundleGradingModel> {

}
