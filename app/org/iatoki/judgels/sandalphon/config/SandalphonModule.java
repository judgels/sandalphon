package org.iatoki.judgels.sandalphon.config;

import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.GitProvider;
import org.iatoki.judgels.LocalFileSystemProvider;
import org.iatoki.judgels.LocalGitProvider;
import org.iatoki.judgels.api.sealtiel.SealtielAPI;
import org.iatoki.judgels.api.sealtiel.SealtielFactory;
import org.iatoki.judgels.play.config.AbstractJudgelsPlayModule;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.services.impls.UserServiceImpl;

public final class SandalphonModule extends AbstractJudgelsPlayModule {

    @Override
    protected void manualBinding() {
        bind(Jophiel.class).toInstance(jophiel());
        bind(SealtielAPI.class).toInstance(sealtielAPI());
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

    private Jophiel jophiel() {
        return new Jophiel(sandalphonProperties().getJophielBaseUrl(), sandalphonProperties().getJophielClientJid(), sandalphonProperties().getJophielClientSecret());
    }

    private SealtielAPI sealtielAPI() {
        String baseUrl = sandalphonProperties().getSealtielBaseUrl();
        String clientJid = sandalphonProperties().getSealtielClientJid();
        String clientSecret = sandalphonProperties().getSealtielClientSecret();

        return SealtielFactory.createSealtiel(baseUrl).connectWithBasicAuth(clientJid, clientSecret);
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
