package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProgrammingProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProgrammingSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingSubmissionDao;
import play.Application;

import java.io.File;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final ProgrammingProblemDao programmingProblemDao;
    private final ProgrammingSubmissionDao programmingSubmissionDao;

    private final ProgrammingProblemService programmingProblemService;

    public Global() {
        this.programmingProblemDao = new ProgrammingProblemHibernateDao();
        this.programmingSubmissionDao = new ProgrammingSubmissionHibernateDao();
        this.programmingProblemService = new ProgrammingProblemServiceImpl(programmingProblemDao, programmingSubmissionDao);
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
