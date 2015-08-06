package org.iatoki.judgels.sandalphon.models.daos.impls.bundle;


import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.AbstractBundleSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleSubmissionModel;

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