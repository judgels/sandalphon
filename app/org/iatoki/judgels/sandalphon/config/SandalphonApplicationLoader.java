package org.iatoki.judgels.sandalphon.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.play.JudgelsPlayProperties;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

public final class SandalphonApplicationLoader extends GuiceApplicationLoader {

    @Override
    public GuiceApplicationBuilder builder(Context context) {
        org.iatoki.judgels.sandalphon.BuildInfo$ buildInfo = org.iatoki.judgels.sandalphon.BuildInfo$.MODULE$;
        JudgelsPlayProperties.buildInstance(buildInfo.name(), buildInfo.version(), ConfigFactory.load());

        Config config = ConfigFactory.load();
        SandalphonProperties.buildInstance(config);

        return super.builder(context);
    }
}
