package org.iatoki.judgels.sandalphon.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.JudgelsProperties;
import org.iatoki.judgels.commons.LocalFileSystemProvider;
import org.iatoki.judgels.commons.LocalGitProvider;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sealtiel.Sealtiel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
        "org.iatoki.judgels.sandalphon.models.daos",
        "org.iatoki.judgels.sandalphon.services",
})
public class PersistenceConfig {

    @Bean
    public JudgelsProperties judgelsProperties() {
        org.iatoki.judgels.sandalphon.BuildInfo$ buildInfo = org.iatoki.judgels.sandalphon.BuildInfo$.MODULE$;
        JudgelsProperties.buildInstance(buildInfo.name(), buildInfo.version(), ConfigFactory.load());
        return JudgelsProperties.getInstance();
    }

    @Bean
    public SandalphonProperties sandalphonProperties() {
        Config config = ConfigFactory.load();
        SandalphonProperties.buildInstance(config);
        return SandalphonProperties.getInstance();
    }

    @Bean
    public Jophiel jophiel() {
        return new Jophiel(sandalphonProperties().getJophielBaseUrl(), sandalphonProperties().getJophielClientJid(), sandalphonProperties().getJophielClientSecret());
    }

    @Bean
    public Sealtiel sealtiel() {
        return new Sealtiel(sandalphonProperties().getSealtielBaseUrl(), sandalphonProperties().getSealtielClientJid(), sandalphonProperties().getSealtielClientSecret());
    }

    @Bean
    public LocalFileSystemProvider problemFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getProblemLocalDir());
    }

    @Bean
    public FileSystemProvider submissionFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getSubmissionLocalDir());
    }

    @Bean
    public LocalFileSystemProvider lessonFileSystemProvider() {
        return new LocalFileSystemProvider(sandalphonProperties().getLessonLocalDir());
    }

    @Bean
    public GitProvider problemGitProvider() {
        return new LocalGitProvider(problemFileSystemProvider());
    }

    @Bean
    public GitProvider lessonGitProvider() {
        return new LocalGitProvider(lessonFileSystemProvider());
    }

    @Bean
    public String gabrielClientJid() {
        return sandalphonProperties().getSealtielGabrielClientJid();
    }
}
