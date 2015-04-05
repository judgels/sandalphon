package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import play.Configuration;
import play.Play;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class SandalphonProperties {
    private static SandalphonProperties INSTANCE;

    private String baseDataDir;

    private SandalphonProperties() {
        Configuration conf = Play.application().configuration();

        verifyConfiguration(conf);

        this.baseDataDir = conf.getString("sandalphon.baseDataDir").replaceAll("\"", "");
    }

    public String getBaseProblemsDirKey() {
        return "problems";
    }

    public String getBaseProblemClonesDirKey() {
        return "problem-clones";
    }

    public String getBaseDataDir() {
        return baseDataDir;
    }

    public File getBaseSubmissionsDir() {
        return new File(baseDataDir, "submissions");
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SandalphonProperties();
        }
        return INSTANCE;
    }

    private static void verifyConfiguration(Configuration configuration) {
        List<String> requiredKeys = ImmutableList.of(
                "jophiel.baseUrl",
                "jophiel.clientJid",
                "jophiel.clientSecret",
                "sealtiel.baseUrl",
                "sealtiel.clientJid",
                "sealtiel.clientSecret",
                "sandalphon.baseDataDir",
                "sealtiel.gabrielClientJid"
        );

        for (String key : requiredKeys) {
            if (configuration.getString(key) == null) {
                throw new RuntimeException("Missing " + key + " property in conf/application.conf");
            }
        }
    }
}
