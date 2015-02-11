package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.domains.ClientModel;
import org.iatoki.judgels.sandalphon.models.domains.ClientModel_;

import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

public final class ClientHibernateDao extends AbstractJudgelsHibernateDao<ClientModel> implements ClientDao {

    public ClientHibernateDao() {
        super(ClientModel.class);
    }

    @Override
    protected List<SingularAttribute<ClientModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(ClientModel_.name);
    }
}