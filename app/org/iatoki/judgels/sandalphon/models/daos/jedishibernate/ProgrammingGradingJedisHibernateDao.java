package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;

import org.iatoki.judgels.sandalphon.models.daos.ProgrammingGradingDao;
import org.iatoki.judgels.sandalphon.models.entities.ProgrammingGradingModel;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("programmingGradingDao")
public final class ProgrammingGradingJedisHibernateDao extends AbstractProgrammingGradingJedisHibernateDao<ProgrammingGradingModel> implements ProgrammingGradingDao {

    @Inject
    public ProgrammingGradingJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, ProgrammingGradingModel.class);
    }

    @Override
    public ProgrammingGradingModel createGradingModel() {
        return new ProgrammingGradingModel();
    }
}
