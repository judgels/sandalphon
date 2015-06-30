package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import org.iatoki.judgels.commons.models.daos.interfaces.BaseJidCacheDao;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivityMessagePusher;
import org.iatoki.judgels.jophiel.services.impls.DefaultUserActivityMessageServiceImpl;
import org.iatoki.judgels.sandalphon.config.ControllerConfig;
import org.iatoki.judgels.sandalphon.config.PersistenceConfig;
import org.iatoki.judgels.sandalphon.controllers.ControllerUtils;
import org.iatoki.judgels.sandalphon.services.JidCacheService;
import org.iatoki.judgels.sandalphon.services.SubmissionService;
import org.iatoki.judgels.sandalphon.services.UserService;
import org.iatoki.judgels.sandalphon.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.sealtiel.Sealtiel;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import play.Application;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {

    private ApplicationContext applicationContext;

    @Override
    public void onStart(Application application) {
        applicationContext = new AnnotationConfigApplicationContext(PersistenceConfig.class, ControllerConfig.class);
        buildServices();
        buildUtils();
        scheduleThreads();
        super.onStart(application);
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return getContextBean(controllerClass).orElse(super.getControllerInstance(controllerClass));
    }

    private <A> Optional<A> getContextBean(Class<A> controllerClass) throws Exception {
        if (applicationContext == null) {
            throw new Exception("Application Context not Initialized");
        } else {
            try {
                return Optional.of(applicationContext.getBean(controllerClass));
            } catch (NoSuchBeanDefinitionException ex) {
                return Optional.empty();
            }
        }
    }

    private void buildServices() {
        JidCacheService.buildInstance(applicationContext.getBean(BaseJidCacheDao.class));
        DefaultUserActivityMessageServiceImpl.buildInstance(applicationContext.getBean(Jophiel.class));
    }

    private void buildUtils() {
        ControllerUtils.buildInstance(applicationContext.getBean(Jophiel.class));
    }

    private void scheduleThreads() {
        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();

        GradingResponsePoller poller = new GradingResponsePoller(scheduler, context, applicationContext.getBean(SubmissionService.class), applicationContext.getBean(Sealtiel.class), TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        UserActivityMessagePusher userActivityMessagePusher = new UserActivityMessagePusher(applicationContext.getBean(Jophiel.class), applicationContext.getBean(UserService.class), UserActivityMessageServiceImpl.getInstance());

        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityMessagePusher, context);
    }
}
