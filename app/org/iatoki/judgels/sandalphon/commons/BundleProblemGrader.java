package org.iatoki.judgels.sandalphon.commons;

import java.io.IOException;

public interface BundleProblemGrader {
    BundleGradingResult gradeBundleProblem(String problemJid, BundleAnswer bundleAnswer) throws IOException;
}
