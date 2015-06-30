package org.iatoki.judgels.sandalphon.models.daos.impls.programming;

import org.iatoki.judgels.sandalphon.models.daos.impls.AbstractGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.GradingModel;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("gradingDao")
public final class GradingHibernateDao extends AbstractGradingHibernateDao<GradingModel> implements GradingDao {

    public GradingHibernateDao() {
        super(GradingModel.class);
    }

    @Override
    public GradingModel createGradingModel() {
        return new GradingModel();
    }
}
