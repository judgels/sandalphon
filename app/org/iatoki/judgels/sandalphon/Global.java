package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivityMessagePusher;
import org.iatoki.judgels.jophiel.services.impls.DefaultUserActivityMessageServiceImpl;
import org.iatoki.judgels.sandalphon.controllers.ControllerUtils;
import org.iatoki.judgels.sandalphon.models.daos.JidCacheDao;
import org.iatoki.judgels.sandalphon.services.SubmissionService;
import org.iatoki.judgels.sandalphon.services.UserService;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.sealtiel.Sealtiel;
import play.Application;
import play.inject.Injector;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.play.Global {

    @Override
    public void onStart(Application application) {
        buildServices(application.injector());
        buildUtils(application.injector());
        scheduleThreads(application.injector());

        super.onStart(application);
    }

    private void buildServices(Injector injector) {
        JidCacheServiceImpl.buildInstance(injector.instanceOf(JidCacheDao.class));
        DefaultUserActivityMessageServiceImpl.buildInstance(injector.instanceOf(Jophiel.class));
    }

    private void buildUtils(Injector injector) {
        ControllerUtils.buildInstance(injector.instanceOf(Jophiel.class));
    }

    private void scheduleThreads(Injector injector) {
        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();

        GradingResponsePoller poller = new GradingResponsePoller(scheduler, context, injector.instanceOf(SubmissionService.class), injector.instanceOf(Sealtiel.class), TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        UserActivityMessagePusher userActivityMessagePusher = new UserActivityMessagePusher(injector.instanceOf(Jophiel.class), injector.instanceOf(UserService.class), UserActivityMessageServiceImpl.getInstance());

        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityMessagePusher, context);
    }
}
