package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;


import org.iatoki.judgels.sandalphon.models.daos.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.BundleSubmissionModel;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("bundleSubmissionDao")
public final class BundleSubmissionJedisHibernateDao extends AbstractBundleSubmissionJedisHibernateDao<BundleSubmissionModel> implements BundleSubmissionDao {

    @Inject
    public BundleSubmissionJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, BundleSubmissionModel.class);
    }

    @Override
    public BundleSubmissionModel createSubmissionModel() {
        return new BundleSubmissionModel();
    }
}
