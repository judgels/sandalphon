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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final Map<Class, Controller> cache;

    private final JidCacheDao jidCacheDao;

    private final Config playConfig;

    public Global() {
        this.playConfig = ConfigFactory.load();

        this.cache = new HashMap<>();
        this.jidCacheDao = new JidCacheHibernateDao();

        JidCacheService.getInstance().setDao(jidCacheDao);
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        SandalphonProperties.getInstance();

        GradingResponsePoller poller = new GradingResponsePoller(createSubmissionService(), createSealtiel());
        UserActivityPusher userActivityPusher = new UserActivityPusher(createUserService(), UserActivityServiceImpl.getInstance());

        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityPusher, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        if (!cache.containsKey(controllerClass)) {
            if (controllerClass.equals(ProblemController.class)) {
                cache.put(controllerClass, new ProblemController(createProblemService()));
            } else if (controllerClass.equals(ProblemAPIController.class)) {
                cache.put(controllerClass, new ProblemAPIController(createProblemService(), createClientService()));
            } else if (controllerClass.equals(ProgrammingProblemAPIController.class)) {
                cache.put(controllerClass, new ProgrammingProblemAPIController(createProblemService(), createProgrammingProblemService(), createClientService(), createGraderService()));
            } else if (controllerClass.equals(ProgrammingProblemController.class)) {
                cache.put(controllerClass, new ProgrammingProblemController(createProblemService(), createProgrammingProblemService()));
            } else if (controllerClass.equals(ProblemPartnerController.class)) {
                cache.put(controllerClass, new ProblemPartnerController(createProblemService()));
            } else if (controllerClass.equals(ProblemStatementController.class)) {
                cache.put(controllerClass, new ProblemStatementController(createProblemService()));
            } else if (controllerClass.equals(ProblemVersionController.class)) {
                cache.put(controllerClass, new ProblemVersionController(createProblemService()));
            } else if (controllerClass.equals(ProblemClientController.class)) {
                cache.put(controllerClass, new ProblemClientController(createProblemService(), createClientService()));
            } else if (controllerClass.equals(ProgrammingProblemGradingController.class)) {
                cache.put(controllerClass, new ProgrammingProblemGradingController(createProblemService(), createProgrammingProblemService()));
            } else if (controllerClass.equals(ProgrammingProblemPartnerController.class)) {
                cache.put(controllerClass, new ProgrammingProblemPartnerController(createProblemService(), createProgrammingProblemService()));
            } else if (controllerClass.equals(ProgrammingProblemStatementController.class)) {
                cache.put(controllerClass, new ProgrammingProblemStatementController(createProblemService(), createProgrammingProblemService()));
            } else if (controllerClass.equals(ProgrammingProblemSubmissionController.class)) {
                cache.put(controllerClass, new ProgrammingProblemSubmissionController(createProblemService(), createProgrammingProblemService(), createSubmissionService()));
            } else if (controllerClass.equals(ClientController.class)) {
                cache.put(controllerClass, new ClientController(createClientService()));
            } else if (controllerClass.equals(GraderController.class)) {
                cache.put(controllerClass, new GraderController(createGraderService()));
            } else if (controllerClass.equals(ApplicationController.class)) {
                cache.put(controllerClass, new ApplicationController(createUserService()));
            } else if (controllerClass.equals(UserController.class)) {
                cache.put(controllerClass, new UserController(createUserService()));
            } else if (controllerClass.equals(JophielClientController.class)) {
                cache.put(controllerClass, new JophielClientController(createUserService()));
            }
        }
        return controllerClass.cast(cache.get(controllerClass));
    }

    private ProblemService createProblemService() {
        FileSystemProvider problemFileSystemProvider = new LocalFileSystemProvider(new File(playConfig.getString("sandalphon.baseDataDir")));
        GitProvider problemGitProvider = new LocalGitProvider(new LocalFileSystemProvider(new File(playConfig.getString("sandalphon.baseDataDir"))));
        ProblemDao problemDao = new ProblemHibernateDao();
        ProblemPartnerDao problemPartnerDao = new ProblemPartnerHibernateDao();

        return new ProblemServiceImpl(problemDao, problemPartnerDao, problemFileSystemProvider, problemGitProvider);
    }

    private ProgrammingProblemService createProgrammingProblemService() {
        FileSystemProvider programmingProblemFSProvider = new LocalFileSystemProvider(new File(playConfig.getString("sandalphon.baseDataDir")));
        return new ProgrammingProblemServiceImpl(programmingProblemFSProvider);
    }

    private SubmissionService createSubmissionService() {
        ProgrammingSubmissionDao submissionDao = new ProgrammingSubmissionHibernateDao();
        GradingDao gradingDao = new GradingHibernateDao();
        Sealtiel sealtiel = createSealtiel();

        return new SubmissionServiceImpl(submissionDao, gradingDao, sealtiel, playConfig.getString("sealtiel.gabrielClientJid"));
    }

    private ClientService createClientService() {
        ClientDao clientDao = new ClientHibernateDao();
        ClientProblemDao clientProblemDao = new ClientProblemHibernateDao();

        return new ClientServiceImpl(clientDao, clientProblemDao);
    }

    private GraderService createGraderService() {
        GraderDao graderDao = new GraderHibernateDao();

        return new GraderServiceImpl(graderDao);
    }

    private UserService createUserService() {
        UserDao userDao = new UserHibernateDao();

        return new UserServiceImpl(userDao);
    }

    private Sealtiel createSealtiel() {
        return new Sealtiel(playConfig.getString("sealtiel.clientJid"), playConfig.getString("sealtiel.clientSecret"), playConfig.getString("sealtiel.baseUrl"));
    }
}
