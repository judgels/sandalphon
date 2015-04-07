package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.LocalFileSystemProvider;
import org.iatoki.judgels.commons.LocalGitProvider;
import org.iatoki.judgels.gabriel.commons.GradingResponsePoller;
import org.iatoki.judgels.gabriel.commons.SubmissionService;
import org.iatoki.judgels.sandalphon.controllers.ApplicationController;
import org.iatoki.judgels.sandalphon.controllers.ClientController;
import org.iatoki.judgels.sandalphon.controllers.GraderController;
import org.iatoki.judgels.sandalphon.controllers.ProblemClientController;
import org.iatoki.judgels.sandalphon.controllers.ProblemController;
import org.iatoki.judgels.sandalphon.controllers.ProblemPartnerController;
import org.iatoki.judgels.sandalphon.controllers.ProblemStatementController;
import org.iatoki.judgels.sandalphon.controllers.ProblemVersionController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemGradingController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemPartnerController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemStatementController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemSubmissionController;
import org.iatoki.judgels.sandalphon.controllers.UserRoleController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProblemAPIController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProgrammingProblemAPIController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemPartnerHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.programming.GraderHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.UserRoleHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.GraderDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserRoleDao;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.programming.GraderServiceImpl;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemServiceImpl;
import org.iatoki.judgels.sandalphon.programming.SubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.programming.GradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.JidCacheHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.programming.ProgrammingSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.JidCacheDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sealtiel.client.Sealtiel;
import play.Application;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final FileSystemProvider fileSystemProvider;
    private final GitProvider gitProvider;
    private final ProblemDao problemDao;
    private final ProblemPartnerDao problemPartnerDao;
    private final ProgrammingSubmissionDao submissionDao;
    private final GradingDao gradingDao;
    private final ClientDao clientDao;
    private final GraderDao graderDao;
    private final ClientProblemDao clientProblemDao;
    private final UserRoleDao userRoleDao;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;
    private final SubmissionService submissionService;
    private final ClientService clientService;
    private final GraderService graderService;
    private final UserRoleService userRoleService;
    private final Sealtiel sealtiel;
    private final JidCacheDao jidCacheDao;

    public Global() {
        Config playConfig = ConfigFactory.load();

        this.fileSystemProvider = new LocalFileSystemProvider(new File(playConfig.getString("sandalphon.baseDataDir")));
        this.gitProvider = new LocalGitProvider(new LocalFileSystemProvider(new File(playConfig.getString("sandalphon.baseDataDir"))));
        this.problemDao = new ProblemHibernateDao();
        this.problemPartnerDao = new ProblemPartnerHibernateDao();
        this.submissionDao = new ProgrammingSubmissionHibernateDao();
        this.gradingDao = new GradingHibernateDao();
        this.clientDao = new ClientHibernateDao();
        this.graderDao = new GraderHibernateDao();
        this.clientProblemDao = new ClientProblemHibernateDao();
        this.userRoleDao = new UserRoleHibernateDao();
        this.sealtiel = new Sealtiel(playConfig.getString("sealtiel.clientJid"), playConfig.getString("sealtiel.clientSecret"), playConfig.getString("sealtiel.baseUrl"));
        this.jidCacheDao = new JidCacheHibernateDao();
        this.problemService = new ProblemServiceImpl(problemDao, problemPartnerDao, fileSystemProvider, gitProvider);
        this.programmingProblemService = new ProgrammingProblemServiceImpl(fileSystemProvider);
        this.submissionService = new SubmissionServiceImpl(submissionDao, gradingDao, sealtiel, playConfig.getString("sealtiel.gabrielClientJid"));
        this.clientService = new ClientServiceImpl(clientDao, clientProblemDao);
        this.graderService = new GraderServiceImpl(graderDao);
        this.userRoleService = new UserRoleServiceImpl(userRoleDao);

        JidCacheService.getInstance().setDao(jidCacheDao);
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        SandalphonProperties.getInstance();

        GradingResponsePoller poller = new GradingResponsePoller(submissionService, sealtiel);

        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        if (controllerClass.equals(ProblemController.class)) {
            return (A) new ProblemController(problemService);
        } else if (controllerClass.equals(ProblemAPIController.class)) {
            return (A) new ProblemAPIController(problemService, clientService);
        } else if (controllerClass.equals(ProgrammingProblemAPIController.class)) {
            return (A) new ProgrammingProblemAPIController(problemService, programmingProblemService, clientService, graderService);
        } else if (controllerClass.equals(ProgrammingProblemController.class)) {
            return (A) new ProgrammingProblemController(problemService, programmingProblemService);
        } else if (controllerClass.equals(ProblemPartnerController.class)) {
            return (A) new ProblemPartnerController(problemService);
        } else if (controllerClass.equals(ProblemStatementController.class)) {
            return (A) new ProblemStatementController(problemService);
        } else if (controllerClass.equals(ProblemVersionController.class)) {
            return (A) new ProblemVersionController(problemService);
        } else if (controllerClass.equals(ProblemClientController.class)) {
            return (A) new ProblemClientController(problemService, clientService);
        } else if (controllerClass.equals(ProgrammingProblemGradingController.class)) {
            return (A) new ProgrammingProblemGradingController(problemService, programmingProblemService);
        } else if (controllerClass.equals(ProgrammingProblemPartnerController.class)) {
            return (A) new ProgrammingProblemPartnerController(problemService, programmingProblemService);
        } else if (controllerClass.equals(ProgrammingProblemStatementController.class)) {
            return (A) new ProgrammingProblemStatementController(problemService, programmingProblemService);
        } else if (controllerClass.equals(ProgrammingProblemSubmissionController.class)) {
            return (A) new ProgrammingProblemSubmissionController(problemService, programmingProblemService, submissionService);
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
