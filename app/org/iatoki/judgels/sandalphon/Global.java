package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.models.daos.DaoFactory;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import play.Application;

public final class Global extends org.iatoki.judgels.commons.Global {

    @Override
    public void onStart(Application application) {
        DaoFactory.getInstance().putDao(ProblemDao.class, new ProblemHibernateDao());
        super.onStart(application);
    }

}
