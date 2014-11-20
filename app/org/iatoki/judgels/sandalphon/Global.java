package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.models.dao.DaoSingletons;
import org.iatoki.judgels.sandalphon.models.dao.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.dao.hibernate.UserHibernateDao;
import org.iatoki.judgels.sandalphon.models.dao.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.dao.interfaces.UserDao;
import play.Application;

public final class Global extends org.iatoki.judgels.commons.Global {

    @Override
    public void onStart(Application application) {
        DaoSingletons.getInstance().putDao(ProblemDao.class, new ProblemHibernateDao());
        DaoSingletons.getInstance().putDao(UserDao.class, new UserHibernateDao());
        super.onStart(application);
    }

}
