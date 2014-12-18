package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.controllers.ProblemController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemHibernateDao;
import play.Application;

public final class Global extends org.iatoki.judgels.commons.Global {

    @Override
    public void onStart(Application application) {
        super.onStart(application);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        if (controllerClass.equals(ProblemController.class)) {
            return (A) new ProblemController(new ProblemServiceImpl(new ProblemHibernateDao()));
        }
        return null;
    }
}
