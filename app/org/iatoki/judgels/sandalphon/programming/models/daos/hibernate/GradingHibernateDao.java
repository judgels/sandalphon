package org.iatoki.judgels.sandalphon.programming.models.daos.hibernate;

import org.iatoki.judgels.gabriel.commons.models.daos.hibernate.AbstractGradingHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.GradingDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.GradingModel;

public final class GradingHibernateDao extends AbstractGradingHibernateDao<GradingModel> implements GradingDao {
    public GradingHibernateDao() {
        super(GradingModel.class);
    }

    @Override
    public GradingModel createGradingModel() {
        return new GradingModel();
    }
}
