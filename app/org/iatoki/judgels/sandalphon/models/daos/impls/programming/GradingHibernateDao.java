package org.iatoki.judgels.sandalphon.models.daos.impls.programming;

import org.iatoki.judgels.sandalphon.commons.models.daos.hibernate.AbstractGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.GradingModel;

public final class GradingHibernateDao extends AbstractGradingHibernateDao<GradingModel> implements GradingDao {
    public GradingHibernateDao() {
        super(GradingModel.class);
    }

    @Override
    public GradingModel createGradingModel() {
        return new GradingModel();
    }
}
