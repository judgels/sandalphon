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

    private File baseDataDir;

    private SandalphonProperties() {
        Configuration conf = Play.application().configuration();

        verifyConfiguration(conf);

        String baseDataDirName = conf.getString("sandalphon.baseDataDir").replaceAll("\"", "");

        File baseDataDir = new File(baseDataDirName);
        if (!baseDataDir.isDirectory()) {
            throw new RuntimeException("sandalphon.baseDataDir: " + baseDataDirName + " does not exist");
        }

        this.baseDataDir = baseDataDir;
    }

    public File getBaseProblemDir() {
        return new File(baseDataDir, "problem");
    }

    public File getProblemDir(String problemJid) {
        return new File(getBaseProblemDir(), problemJid);
    }

    public File getProblemStatementDir(String problemJid) {
        return new File(getProblemDir(problemJid), "statement");
    }

    public File getProblemMediaDir(String problemJid) {
        return new File(getProblemDir(problemJid), "media");
    }

    public File getBaseProgrammingDir() {
        return new File(baseDataDir, "programming");
    }

    public File getProgrammingDir(String problemJid) {
        return new File(getBaseProgrammingDir(), problemJid);
    }

    public File getProgrammingGradingDir(String problemJid) {
        return new File(getProgrammingDir(problemJid), "grading");
    }

    public File getProgrammingGradingTestDataDir(String problemJid) {
        return new File(getProgrammingGradingDir(problemJid), "testdata");
    }

    public File getProgrammingGradingHelperDir(String problemJid) {
        return new File(getProgrammingGradingDir(problemJid), "helper");
    }

    public File getBaseProgrammingSubmissionDir() {
        return new File(getBaseProgrammingDir(), "submission");
    }

    public File getProgrammingSubmissionDir(String problemJid) {
        return new File(getBaseProgrammingSubmissionDir(), problemJid);
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SandalphonProperties();

            try {
                FileUtils.forceMkdir(INSTANCE.getBaseProblemDir());
                FileUtils.forceMkdir(INSTANCE.getBaseProgrammingDir());
                FileUtils.forceMkdir(INSTANCE.getBaseProgrammingSubmissionDir());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
