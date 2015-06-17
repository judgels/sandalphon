package org.iatoki.judgels.sandalphon.models.daos.impls.bundle;

import org.iatoki.judgels.sandalphon.models.daos.impls.AbstractBundleGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleGradingModel;

public final class BundleGradingHibernateDao extends AbstractBundleGradingHibernateDao<BundleGradingModel> implements BundleGradingDao {
    public BundleGradingHibernateDao() {
        super(BundleGradingModel.class);
    }

    @Override
    public BundleGradingModel createGradingModel() {
        return new BundleGradingModel();
    }
}
