package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;

import org.iatoki.judgels.sandalphon.models.daos.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.entities.BundleGradingModel;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("bundleGradingDao")
public final class BundleGradingJedisHibernateDao extends AbstractBundleGradingJedisHibernateDao<BundleGradingModel> implements BundleGradingDao {

    @Inject
    public BundleGradingJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, BundleGradingModel.class);
    }

    @Override
    public BundleGradingModel createGradingModel() {
        return new BundleGradingModel();
    }
}
