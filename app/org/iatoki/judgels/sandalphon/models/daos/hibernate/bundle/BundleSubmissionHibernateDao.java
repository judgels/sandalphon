package org.iatoki.judgels.sandalphon.models.daos.hibernate.bundle;


import org.iatoki.judgels.sandalphon.commons.models.daos.hibernate.AbstractBundleSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.bundle.BundleSubmissionModel;

public final class BundleSubmissionHibernateDao extends AbstractBundleSubmissionHibernateDao<BundleSubmissionModel> implements BundleSubmissionDao {
    public BundleSubmissionHibernateDao() {
        super(BundleSubmissionModel.class);
    }

    @Override
    public BundleSubmissionModel createSubmissionModel() {
        return new BundleSubmissionModel();
    }
}
