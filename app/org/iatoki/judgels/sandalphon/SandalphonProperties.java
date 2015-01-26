package org.iatoki.judgels.sandalphon;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class SandalphonProperties {
    private static SandalphonProperties INSTANCE;

    private final File problemDir;
    private final File submissionDir;

    private SandalphonProperties(File baseDir) {
        try {
            this.problemDir = new File(baseDir, "problem");
            FileUtils.forceMkdir(problemDir);
            this.submissionDir = new File(baseDir, "submission");
            FileUtils.forceMkdir(submissionDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create folder inside " + baseDir.getAbsolutePath());
        }
    }

    public File getProblemDir() {
        return this.problemDir;
    }

    public File getSubmissionDir() {
        return this.submissionDir;
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
