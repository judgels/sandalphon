package org.iatoki.judgels.sandalphon.models.daos.hibernate.programming;

import org.iatoki.judgels.gabriel.commons.models.daos.hibernate.AbstractGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.GradingModel;

public final class GradingHibernateDao extends AbstractGradingHibernateDao<GradingModel> implements GradingDao {
    public GradingHibernateDao() {
        super(GradingModel.class);
    }

    @Override
    public GradingModel createGradingModel() {
        return new GradingModel();
    }
}
