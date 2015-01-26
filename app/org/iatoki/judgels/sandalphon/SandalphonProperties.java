package org.iatoki.judgels.sandalphon;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class SandalphonProperties {
    private static SandalphonProperties INSTANCE;

    private final File problemsDir;
    private final File submissionsDir;

    private SandalphonProperties(File baseDir) {
        try {
            this.problemsDir = new File(baseDir, "problems");
            FileUtils.forceMkdir(problemsDir);
            this.submissionsDir = new File(baseDir, "submissions");
            FileUtils.forceMkdir(submissionsDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create folder inside " + baseDir.getAbsolutePath());
        }
    }

    public File getProblemsDir() {
        return this.problemsDir;
    }

    public File getSubmissionsDir() {
        return this.submissionsDir;
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            String baseDirName = play.Play.application().configuration().getString("sandalphon.baseDir");
            if (baseDirName == null) {
                throw new RuntimeException("Missing sandalphon.baseDir properties in conf/application.conf");
            }

            baseDirName = baseDirName.replaceAll("\"", "");

            File baseDir = new File(baseDirName);
            if (!baseDir.isDirectory()) {
                throw new RuntimeException("sandalphon.baseDir: " + baseDirName + " does not exist");
            }

            INSTANCE = new SandalphonProperties(baseDir);
        }
        return INSTANCE;
    }
}
