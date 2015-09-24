package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielPublicAPI;
import org.iatoki.judgels.api.sealtiel.SealtielClientAPI;
import org.iatoki.judgels.jophiel.controllers.JophielClientControllerUtils;
import org.iatoki.judgels.jophiel.runnables.UserActivityMessagePusher;
import org.iatoki.judgels.jophiel.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.play.AbstractGlobal;
import org.iatoki.judgels.play.services.BaseDataMigrationService;
import org.iatoki.judgels.sandalphon.controllers.SandalphonControllerUtils;
import org.iatoki.judgels.sandalphon.models.daos.ActivityLogDao;
import org.iatoki.judgels.sandalphon.models.daos.JidCacheDao;
import org.iatoki.judgels.sandalphon.runnables.GradingResponsePoller;
import org.iatoki.judgels.sandalphon.services.ProgrammingSubmissionService;
import org.iatoki.judgels.sandalphon.services.impls.ActivityLogServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.SandalphonDataMigrationServiceImpl;
import play.Application;
import play.inject.Injector;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public final class Global extends AbstractGlobal {

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        buildServices(application.injector());
        buildUtils(application.injector());
        scheduleThreads(application.injector());
    }

    @Override
    protected BaseDataMigrationService getDataMigrationService() {
        return new SandalphonDataMigrationServiceImpl();
    }

    private void buildServices(Injector injector) {
        JidCacheServiceImpl.buildInstance(injector.instanceOf(JidCacheDao.class));
        ActivityLogServiceImpl.buildInstance(injector.instanceOf(ActivityLogDao.class));
        UserActivityMessageServiceImpl.buildInstance();
    }

    private void buildUtils(Injector injector) {
        JophielClientControllerUtils.buildInstance(SandalphonProperties.getInstance().getJophielBaseUrl());
        SandalphonControllerUtils.buildInstance(injector.instanceOf(JophielClientAPI.class), injector.instanceOf(JophielPublicAPI.class));
    }

    private void scheduleThreads(Injector injector) {
        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();

        GradingResponsePoller poller = new GradingResponsePoller(scheduler, context, injector.instanceOf(ProgrammingSubmissionService.class), injector.instanceOf(SealtielClientAPI.class), TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        UserActivityMessagePusher userActivityMessagePusher = new UserActivityMessagePusher(injector.instanceOf(JophielClientAPI.class), UserActivityMessageServiceImpl.getInstance());

        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityMessagePusher, context);
    }
}
