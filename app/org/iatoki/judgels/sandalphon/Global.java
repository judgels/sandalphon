package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.JudgelsProperties;
import org.iatoki.judgels.commons.LocalFileSystemProvider;
import org.iatoki.judgels.commons.LocalGitProvider;
import org.iatoki.judgels.gabriel.commons.GradingResponsePoller;
import org.iatoki.judgels.gabriel.commons.SubmissionService;
import org.iatoki.judgels.jophiel.commons.UserActivityPusher;
import org.iatoki.judgels.jophiel.commons.controllers.JophielClientController;
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
import org.iatoki.judgels.sandalphon.controllers.UserController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProblemAPIController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProgrammingProblemAPIController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ProblemPartnerHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.programming.GraderHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.UserHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.GraderDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserDao;
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
import play.mvc.Controller;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {
    private ClientDao clientDao;
    private ClientProblemDao clientProblemDao;
    private JidCacheDao jidCacheDao;
    private ProblemDao problemDao;
    private ProblemPartnerDao problemPartnerDao;
    private UserDao userDao;
    private GraderDao graderDao;
    private GradingDao gradingDao;
    private ProgrammingSubmissionDao programmingSubmissionDao;

    private SandalphonProperties sandalphonProps;

    private Sealtiel sealtiel;

    private LocalFileSystemProvider problemFileSystemProvider;
    private FileSystemProvider submissionFileSystemProvider;

    private GitProvider problemGitProvider;

    private ClientService clientService;
    private GraderService graderService;
    private UserService userService;
    private ProblemService problemService;
    private ProgrammingProblemService programmingProblemService;
    private SubmissionService submissionService;

    private Map<Class<?>, Controller> controllersRegistry;

    @Override
    public void onStart(Application application) {
        buildDaos();
        buildProperties();
        buildSealtiel();
        buildFileProviders();
        buildGitProviders();
        buildServices();
        buildControllers();
        scheduleThreads();
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return controllerClass.cast(controllersRegistry.get(controllerClass));
    }

    private void buildDaos() {
        clientDao = new ClientHibernateDao();
        clientProblemDao = new ClientProblemHibernateDao();
        jidCacheDao = new JidCacheHibernateDao();
        problemDao = new ProblemHibernateDao();
        problemPartnerDao = new ProblemPartnerHibernateDao();
        userDao = new UserHibernateDao();
        graderDao = new GraderHibernateDao();
        gradingDao = new GradingHibernateDao();
        programmingSubmissionDao = new ProgrammingSubmissionHibernateDao();
    }

    private void buildProperties() {
        Config config = ConfigFactory.load();

        org.iatoki.judgels.sandalphon.BuildInfo$ buildInfo = org.iatoki.judgels.sandalphon.BuildInfo$.MODULE$;
        JudgelsProperties.buildInstance(buildInfo.name(), buildInfo.version(), config);

        SandalphonProperties.buildInstance(config);
        sandalphonProps = SandalphonProperties.getInstance();
    }

    private void buildSealtiel() {
        sealtiel = new Sealtiel(sandalphonProps.getSealtielClientJid(), sandalphonProps.getSealtielClientSecret(), sandalphonProps.getSealtielBaseUrl());
    }

    private void buildFileProviders() {
        problemFileSystemProvider = new LocalFileSystemProvider(sandalphonProps.getProblemLocalDir());
        submissionFileSystemProvider = new LocalFileSystemProvider(sandalphonProps.getSubmissionLocalDir());
    }

    private void buildGitProviders() {
        problemGitProvider = new LocalGitProvider(problemFileSystemProvider);
    }

    private void buildServices() {
        clientService = new ClientServiceImpl(clientDao, clientProblemDao);
        graderService = new GraderServiceImpl(graderDao);
        userService = new UserServiceImpl(userDao);
        problemService = new ProblemServiceImpl(problemDao, problemPartnerDao, problemFileSystemProvider, problemGitProvider);
        programmingProblemService = new ProgrammingProblemServiceImpl(problemFileSystemProvider);
        submissionService = new SubmissionServiceImpl(programmingSubmissionDao, gradingDao, sealtiel, SandalphonProperties.getInstance().getSealtielGabrielClientJid());

        JidCacheService.getInstance().setDao(jidCacheDao);
    }

    private void buildControllers() {
        controllersRegistry = ImmutableMap.<Class<?>, Controller> builder()
                .put(ApplicationController.class, new ApplicationController(userService))
                .put(JophielClientController.class, new JophielClientController(userService))
                .put(ClientController.class, new ClientController(clientService))
                .put(GraderController.class, new GraderController(graderService))
                .put(ProblemClientController.class, new ProblemClientController(problemService, clientService))
                .put(ProblemController.class, new ProblemController(problemService))
                .put(ProblemPartnerController.class, new ProblemPartnerController(problemService))
                .put(ProblemStatementController.class, new ProblemStatementController(problemService))
                .put(ProblemVersionController.class, new ProblemVersionController(problemService))
                .put(ProgrammingProblemController.class, new ProgrammingProblemController(problemService, programmingProblemService))
                .put(ProgrammingProblemGradingController.class, new ProgrammingProblemGradingController(problemService, programmingProblemService))
                .put(ProgrammingProblemPartnerController.class, new ProgrammingProblemPartnerController(problemService, programmingProblemService))
                .put(ProgrammingProblemStatementController.class, new ProgrammingProblemStatementController(problemService, programmingProblemService))
                .put(ProgrammingProblemSubmissionController.class, new ProgrammingProblemSubmissionController(problemService, programmingProblemService, submissionService, submissionFileSystemProvider))
                .put(ProblemAPIController.class, new ProblemAPIController(problemService, clientService))
                .put(ProgrammingProblemAPIController.class, new ProgrammingProblemAPIController(problemService, programmingProblemService, clientService, graderService))
                .put(UserController.class, new UserController(userService))
                .build();
    }

    private void scheduleThreads() {
        GradingResponsePoller poller = new GradingResponsePoller(submissionService, sealtiel, TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        UserActivityPusher userActivityPusher = new UserActivityPusher(userService, UserActivityServiceImpl.getInstance());

        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();

        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityPusher, context);
    }
}
