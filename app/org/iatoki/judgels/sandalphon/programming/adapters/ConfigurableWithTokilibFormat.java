package org.iatoki.judgels.sandalphon.programming.adapters;

import org.iatoki.judgels.FileInfo;
import org.iatoki.judgels.gabriel.GradingConfig;

import java.util.List;

public interface ConfigurableWithTokilibFormat {
    GradingConfig updateConfigWithTokilibFormat(GradingConfig config, List<FileInfo> testDataFiles);
}
