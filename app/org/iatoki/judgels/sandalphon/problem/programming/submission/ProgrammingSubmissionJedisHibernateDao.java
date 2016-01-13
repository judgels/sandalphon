package org.iatoki.judgels.sandalphon.problem.programming.submission;

import org.iatoki.judgels.sandalphon.models.daos.jedishibernate.AbstractProgrammingSubmissionJedisHibernateDao;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ProgrammingSubmissionJedisHibernateDao extends AbstractProgrammingSubmissionJedisHibernateDao<ProgrammingSubmissionModel> implements ProgrammingSubmissionDao {

    @Inject
    public ProgrammingSubmissionJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, ProgrammingSubmissionModel.class);
    }

    @Override
    public ProgrammingSubmissionModel createSubmissionModel() {
        return new ProgrammingSubmissionModel();
    }
}
