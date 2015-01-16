package org.iatoki.judgels.sandalphon;

import java.io.File;

public final class SandalphonProperties {
    private static SandalphonProperties INSTANCE;

    private final File baseDir;

    private SandalphonProperties(File baseDir) {
        this.baseDir = baseDir;
    }

    public File getProblemsDir() {
        return new File(baseDir, "problems");
    }

    public File getSubmissionsDir() {
        return new File(baseDir, "submissions");
    }

    public static SandalphonProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SandalphonProperties(new File("/Users/fushar/sandalphon-data"));
        }
        return INSTANCE;
    }
}
