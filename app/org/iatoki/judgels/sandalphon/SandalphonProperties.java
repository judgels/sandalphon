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

    private File problemDir;
    private File submissionDir;

    private SandalphonProperties() {

    }

    public File getProblemDir() {
        return this.problemDir;
    }

    public File getSubmissionDir() {
        return this.submissionDir;
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SandalphonProperties();

            Configuration conf = Play.application().configuration();

            verifyConfiguration(conf);

            String baseDirName = conf.getString("sandalphon.baseDir").replaceAll("\"", "");

            File baseDir = new File(baseDirName);
            if (!baseDir.isDirectory()) {
                throw new RuntimeException("sandalphon.baseDir: " + baseDirName + " does not exist");
            }

            try {
                INSTANCE.problemDir = new File(baseDir, "problem");
                FileUtils.forceMkdir(INSTANCE.problemDir);
                INSTANCE.submissionDir = new File(baseDir, "submission");
                FileUtils.forceMkdir(INSTANCE.submissionDir);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create folder inside " + baseDir.getAbsolutePath());
            }
        }
        return INSTANCE;
    }

    private static void verifyConfiguration(Configuration configuration) {
        List<String> requiredKeys = ImmutableList.of(
                "jophiel.baseUrl",
                "jophiel.clientId",
                "jophiel.clientSecret",
                "sealtiel.baseUrl",
                "sealtiel.clientChannel",
                "sealtiel.clientId",
                "sealtiel.clientSecret",
                "sandalphon.baseDir"
        );

        for (String key : requiredKeys) {
            if (configuration.getString(key) == null) {
                throw new RuntimeException("Missing " + key + " property in conf/application.conf");
            }
        }
    }
}
