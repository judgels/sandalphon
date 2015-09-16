package org.iatoki.judgels.sandalphon.models.daos.hibernate;


import org.iatoki.judgels.sandalphon.models.daos.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.BundleSubmissionModel;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("bundleSubmissionDao")
public final class BundleSubmissionHibernateDao extends AbstractBundleSubmissionHibernateDao<BundleSubmissionModel> implements BundleSubmissionDao {

    public BundleSubmissionHibernateDao() {
        super(BundleSubmissionModel.class);
    }

    @Override
    public BundleSubmissionModel createSubmissionModel() {
        return new BundleSubmissionModel();
    }
}
