package org.iatoki.judgels.sandalphon.config;

import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.GitProvider;
import org.iatoki.judgels.LocalFileSystemProvider;
import org.iatoki.judgels.LocalGitProvider;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielFactory;
import org.iatoki.judgels.api.jophiel.JophielPublicAPI;
import org.iatoki.judgels.api.sealtiel.SealtielClientAPI;
import org.iatoki.judgels.api.sealtiel.SealtielFactory;
import org.iatoki.judgels.play.config.AbstractJudgelsPlayModule;
import org.iatoki.judgels.jophiel.JophielAuthAPI;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.services.impls.UserServiceImpl;

public final class SandalphonModule extends AbstractJudgelsPlayModule {

    @Override
    protected void manualBinding() {
        bind(JophielAuthAPI.class).toInstance(jophielAuthAPI());
        bind(JophielClientAPI.class).toInstance(jophielClientAPI());
        bind(JophielPublicAPI.class).toInstance(jophielPublicAPI());
        bind(SealtielClientAPI.class).toInstance(sealtielClientAPI());
        bind(FileSystemProvider.class).annotatedWith(ProblemFileSystemProvider.class).toInstance(problemFileSystemProvider());
        bind(FileSystemProvider.class).annotatedWith(SubmissionFileSystemProvider.class).toInstance(submissionFileSystemProvider());
        bind(FileSystemProvider.class).annotatedWith(LessonFileSystemProvider.class).toInstance(lessonFileSystemProvider());
        bind(GitProvider.class).annotatedWith(ProblemGitProvider.class).toInstance(problemGitProvider());
        bind(GitProvider.class).annotatedWith(LessonGitProvider.class).toInstance(lessonGitProvider());
        bindConstant().annotatedWith(GabrielClientJid.class).to(gabrielClientJid());
        bind(BaseUserService.class).to(UserServiceImpl.class);
    }

    @Override
    protected String getDaosImplPackage() {
        return "org.iatoki.judgels.sandalphon.models.daos.impls";
    }

    @Override
    protected String getServicesImplPackage() {
        return "org.iatoki.judgels.sandalphon.services.impls";
    }

    private SandalphonProperties sandalphonProperties() {
        return SandalphonProperties.getInstance();
    }

    private JophielAuthAPI jophielAuthAPI() {
        return new JophielAuthAPI(sandalphonProperties().getJophielBaseUrl(), sandalphonProperties().getJophielClientJid(), sandalphonProperties().getJophielClientSecret());
    }

    private JophielClientAPI jophielClientAPI() {
        return JophielFactory.createJophiel(sandalphonProperties().getJophielBaseUrl()).connectToClientAPI(sandalphonProperties().getJophielClientJid(), sandalphonProperties().getJophielClientSecret());
    }

    private JophielPublicAPI jophielPublicAPI() {
        return JophielFactory.createJophiel(sandalphonProperties().getJophielBaseUrl()).connectToPublicAPI();
    }

    private SealtielClientAPI sealtielClientAPI() {
        return SealtielFactory.createSealtiel(sandalphonProperties().getSealtielBaseUrl()).connectToClientAPI(sandalphonProperties().getSealtielClientJid(), sandalphonProperties().getSealtielClientSecret());
    }

    private LocalFileSystemProvider problemFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getProblemLocalDir());
    }

    private FileSystemProvider submissionFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getSubmissionLocalDir());
    }

    private LocalFileSystemProvider lessonFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getLessonLocalDir());
    }

    private GitProvider problemGitProvider() {
        return new LocalGitProvider(problemFileSystemProvider());
    }

    private GitProvider lessonGitProvider() {
        return new LocalGitProvider(lessonFileSystemProvider());
    }

    private String gabrielClientJid() {
        return sandalphonProperties().getSealtielGabrielClientJid();
    }
}
