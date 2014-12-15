package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.models.daos.DaoFactory;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProgrammingProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import play.Application;

public final class Global extends org.iatoki.judgels.commons.Global {

    @Override
    public void onStart(Application application) {
        DaoFactory.getInstance().putDao(ProgrammingProblemDao.class, new ProgrammingProblemHibernateDao());
        super.onStart(application);
    }

}
