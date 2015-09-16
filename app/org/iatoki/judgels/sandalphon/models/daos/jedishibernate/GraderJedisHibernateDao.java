package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.models.daos.impls.AbstractJudgelsJedisHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.GraderDao;
import org.iatoki.judgels.sandalphon.models.entities.GraderModel;
import org.iatoki.judgels.sandalphon.models.entities.GraderModel_;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

@Singleton
@Named("graderDao")
public final class GraderJedisHibernateDao extends AbstractJudgelsJedisHibernateDao<GraderModel> implements GraderDao {

    @Inject
    public GraderJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, GraderModel.class);
    }

    @Override
    protected List<SingularAttribute<GraderModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(GraderModel_.name);
    }
}
