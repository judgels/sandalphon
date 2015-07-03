package org.iatoki.judgels.sandalphon.config;

import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.LocalFileSystemProvider;
import org.iatoki.judgels.commons.LocalGitProvider;
import org.iatoki.judgels.commons.config.JudgelsAbstractModule;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.services.impls.UserServiceImpl;
import org.iatoki.judgels.sealtiel.Sealtiel;

public final class SandalphonModule extends JudgelsAbstractModule {

    @Override
    protected void manualBinding() {
        bind(Jophiel.class).toInstance(jophiel());
        bind(Sealtiel.class).toInstance(sealtiel());
        bind(FileSystemProvider.class).annotatedWith(ProblemFile.class).toInstance(problemFileSystemProvider());
        bind(FileSystemProvider.class).annotatedWith(SubmissionFile.class).toInstance(submissionFileSystemProvider());
        bind(FileSystemProvider.class).annotatedWith(LessonFile.class).toInstance(lessonFileSystemProvider());
        bind(GitProvider.class).annotatedWith(ProblemGit.class).toInstance(problemGitProvider());
        bind(GitProvider.class).annotatedWith(LessonGit.class).toInstance(lessonGitProvider());
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

    private Sealtiel sealtiel() {
        return new Sealtiel(sandalphonProperties().getSealtielBaseUrl(), sandalphonProperties().getSealtielClientJid(), sandalphonProperties().getSealtielClientSecret());
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
