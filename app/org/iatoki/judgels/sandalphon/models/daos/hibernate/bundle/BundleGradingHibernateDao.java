package org.iatoki.judgels.sandalphon.models.daos.hibernate.bundle;

import org.iatoki.judgels.sandalphon.commons.models.daos.hibernate.AbstractBundleGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.domains.bundle.BundleGradingModel;

public final class BundleGradingHibernateDao extends AbstractBundleGradingHibernateDao<BundleGradingModel> implements BundleGradingDao {
    public BundleGradingHibernateDao() {
        super(BundleGradingModel.class);
    }

    @Override
    public BundleGradingModel createGradingModel() {
        return new BundleGradingModel();
    }
}
