package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.controllers.ProblemController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProgrammingProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import play.Application;

import java.io.File;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final ProblemDao problemDao;
    private final ProgrammingProblemDao programmingProblemDao;

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public Global() {
        this.problemDao = new ProblemHibernateDao();
        this.programmingProblemDao = new ProgrammingProblemHibernateDao();

        this.problemService = new ProblemServiceImpl(problemDao);
        this.programmingProblemService = new ProgrammingProblemServiceImpl(problemDao, programmingProblemDao, new File("/Users/fushar/jagoparah"));
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        if (controllerClass.equals(ProblemController.class)) {
            return (A) new ProblemController(problemService, programmingProblemService);
        } else if (controllerClass.equals(ProgrammingProblemController.class)) {
            return (A) new ProgrammingProblemController(problemService, programmingProblemService);
        } else {
            return controllerClass.newInstance();
        }
    }
}
