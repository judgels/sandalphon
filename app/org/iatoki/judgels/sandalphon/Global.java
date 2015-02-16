package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.gabriel.commons.GradingResponsePoller;
import org.iatoki.judgels.sandalphon.commons.SubmissionUpdaterService;
import org.iatoki.judgels.sandalphon.controllers.ApplicationController;
import org.iatoki.judgels.sandalphon.controllers.ClientController;
import org.iatoki.judgels.sandalphon.controllers.GraderController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.controllers.UserRoleController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.GraderHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.UserRoleHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.GraderDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserRoleDao;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.programming.ProblemServiceImpl;
import org.iatoki.judgels.sandalphon.programming.ProblemSubmissionService;
import org.iatoki.judgels.sandalphon.programming.ProblemSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.programming.SubmissionUpdaterServiceImpl;
import org.iatoki.judgels.sandalphon.programming.models.daos.hibernate.JidCacheHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.hibernate.ProblemSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.JidCacheDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sealtiel.client.Sealtiel;
import play.Application;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final ProblemDao problemDao;
    private final ProblemSubmissionDao submissionDao;
    private final ClientDao clientDao;
    private final GraderDao graderDao;
    private final ClientProblemDao clientProblemDao;
    private final UserRoleDao userRoleDao;
    private final SubmissionUpdaterService submissionUpdaterService;
    private final ProblemService problemService;
    private final ProblemSubmissionService submissionService;
    private final ClientService clientService;
    private final GraderService graderService;
    private final UserRoleService userRoleService;
    private final Sealtiel sealtiel;
    private final JidCacheDao jidCacheDao;

    public Global() {
        Config playConfig = ConfigFactory.load();

        this.problemDao = new ProblemHibernateDao();
        this.submissionDao = new ProblemSubmissionHibernateDao();
        this.clientDao = new ClientHibernateDao();
        this.graderDao = new GraderHibernateDao();
        this.clientProblemDao = new ClientProblemHibernateDao();
        this.userRoleDao = new UserRoleHibernateDao();
        this.sealtiel = new Sealtiel(playConfig.getString("sealtiel.clientJid"), playConfig.getString("sealtiel.clientSecret"), playConfig.getString("sealtiel.baseUrl"));
        this.jidCacheDao = new JidCacheHibernateDao();
        this.problemService = new ProblemServiceImpl(problemDao);
        this.submissionService = new ProblemSubmissionServiceImpl(submissionDao, sealtiel);
        this.submissionUpdaterService = new SubmissionUpdaterServiceImpl(submissionDao);
        this.clientService = new ClientServiceImpl(clientDao, clientProblemDao);
        this.graderService = new GraderServiceImpl(graderDao);
        this.userRoleService = new UserRoleServiceImpl(userRoleDao);

        JidCacheService.getInstance().setDao(jidCacheDao);
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        GradingResponsePoller poller = new GradingResponsePoller(submissionUpdaterService, sealtiel);

        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
         if (controllerClass.equals(ProgrammingProblemController.class)) {
            return (A) new ProgrammingProblemController(problemService, submissionService, clientService, graderService);
         } else if (controllerClass.equals(ClientController.class)) {
            return (A) new ClientController(clientService);
         } else if (controllerClass.equals(GraderController.class)) {
             return (A) new GraderController(graderService);
         } else if (controllerClass.equals(ApplicationController.class)) {
            return (A) new ApplicationController(userRoleService);
         } else if (controllerClass.equals(UserRoleController.class)) {
            return (A) new UserRoleController(userRoleService);
         } else {
            return controllerClass.newInstance();
         }
    }
}
