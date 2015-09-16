package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.models.daos.impls.AbstractJudgelsJedisHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientDao;
import org.iatoki.judgels.sandalphon.models.entities.ClientModel;
import org.iatoki.judgels.sandalphon.models.entities.ClientModel_;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

@Singleton
@Named("clientDao")
public final class ClientJedisHibernateDao extends AbstractJudgelsJedisHibernateDao<ClientModel> implements ClientDao {

    @Inject
    public ClientJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, ClientModel.class);
    }

    @Override
    protected List<SingularAttribute<ClientModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(ClientModel_.name);
    }
}
