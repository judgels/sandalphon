package org.iatoki.judgels.sandalphon;

import org.apache.commons.io.FileUtils;
import play.Configuration;

import java.io.File;
import java.io.IOException;

public final class SandalphonProperties {
    private static SandalphonProperties INSTANCE;

    private final Configuration conf;
    private final String confLocation;

    private File sandalphonBaseDataDir;
    private File submissionLocalDir;


    private String jophielBaseUrl;
    private String jophielClientJid;
    private String jophielClientSecret;

    private String sealtielBaseUrl;
    private String sealtielClientJid;
    private String sealtielClientSecret;
    private String sealtielGabrielClientJid;

    private SandalphonProperties(Configuration conf, String confLocation) {
        this.conf = conf;
        this.confLocation = confLocation;
    }

    public static synchronized void buildInstance(Configuration conf, String confLocation) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("SandalphonProperties instance has already been built");
        }

        INSTANCE = new SandalphonProperties(conf, confLocation);
        INSTANCE.build();
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("SandalphonProperties instance has not been built");
        }
        return INSTANCE;
    }

    public String getJophielBaseUrl() {
        return jophielBaseUrl;
    }

    public String getJophielClientJid() {
        return jophielClientJid;
    }

    public String getJophielClientSecret() {
        return jophielClientSecret;
    }

    public String getSealtielBaseUrl() {
        return sealtielBaseUrl;
    }

    public String getSealtielClientJid() {
        return sealtielClientJid;
    }

    public String getSealtielClientSecret() {
        return sealtielClientSecret;
    }

    public String getSealtielGabrielClientJid() {
        return sealtielGabrielClientJid;
    }

    public File getSubmissionLocalDir() {
        return submissionLocalDir;
    }

    public File getProblemLocalDir() {
        return sandalphonBaseDataDir;
    }

    public String getBaseProblemsDirKey() {
        return "problems";
    }

    public String getBaseProblemClonesDirKey() {
        return "problem-clones";
    }

    private void build() {
        sandalphonBaseDataDir = requireDirectoryValue("sandalphon.baseDataDir");

        try {
            submissionLocalDir = new File(sandalphonBaseDataDir, "submissions");
            FileUtils.forceMkdir(submissionLocalDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        jophielBaseUrl = requireStringValue("jophiel.baseUrl");
        jophielClientJid = requireStringValue("jophiel.clientJid");
        jophielClientSecret = requireStringValue("jophiel.clientSecret");

        sealtielBaseUrl = requireStringValue("sealtiel.baseUrl");
        sealtielClientJid = requireStringValue("sealtiel.clientJid");
        sealtielClientSecret = requireStringValue("sealtiel.clientSecret");
        sealtielGabrielClientJid = requireStringValue("sealtiel.gabrielClientJid");
    }

    private String getStringValue(String key) {
        return conf.getString(key);
    }

    private String requireStringValue(String key) {
        String value = getStringValue(key);
        if (value == null) {
            throw new RuntimeException("Missing " + key + " property in " + confLocation);
        }
        return value;
    }

    private File requireDirectoryValue(String key) {
        String filename = getStringValue(key);

        File dir = new File(filename);
        if (!dir.isDirectory()) {
            throw new RuntimeException("Directory " + dir.getAbsolutePath() + " does not exist");
        }
        return dir;
    }
}
