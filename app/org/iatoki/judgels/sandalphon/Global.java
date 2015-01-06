package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProgrammingProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import play.Application;

import java.io.File;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final ProgrammingProblemDao programmingProblemDao;

    private final ProgrammingProblemService programmingProblemService;

    public Global() {
        this.programmingProblemDao = new ProgrammingProblemHibernateDao();

        this.programmingProblemService = new ProgrammingProblemServiceImpl(programmingProblemDao, new File("/Users/fushar/jagoparah"));
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
         if (controllerClass.equals(ProgrammingProblemController.class)) {
            return (A) new ProgrammingProblemController(programmingProblemService);
        } else {
            return controllerClass.newInstance();
        }
    }
}
