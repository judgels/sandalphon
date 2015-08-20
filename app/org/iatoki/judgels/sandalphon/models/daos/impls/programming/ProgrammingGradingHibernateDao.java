package org.iatoki.judgels.sandalphon.models.daos.impls.programming;

import org.iatoki.judgels.sandalphon.models.daos.impls.AbstractProgrammingGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingGradingDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.ProgrammingGradingModel;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("programmingGradingDao")
public final class ProgrammingGradingHibernateDao extends AbstractProgrammingGradingHibernateDao<ProgrammingGradingModel> implements ProgrammingGradingDao {

    public ProgrammingGradingHibernateDao() {
        super(ProgrammingGradingModel.class);
    }

    @Override
    public ProgrammingGradingModel createGradingModel() {
        return new ProgrammingGradingModel();
    }
}
