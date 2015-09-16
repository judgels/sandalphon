package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.sandalphon.models.daos.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.entities.BundleGradingModel;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("bundleGradingDao")
public final class BundleGradingHibernateDao extends AbstractBundleGradingHibernateDao<BundleGradingModel> implements BundleGradingDao {

    public BundleGradingHibernateDao() {
        super(BundleGradingModel.class);
    }

    @Override
    public BundleGradingModel createGradingModel() {
        return new BundleGradingModel();
    }
}
