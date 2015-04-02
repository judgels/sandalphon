package org.iatoki.judgels.sandalphon.programming.adapters;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.gabriel.GradingConfig;

import java.io.File;
import java.util.List;

public interface ConfigurableWithAutoPopulation {
    GradingConfig updateConfigWithAutoPopulation(GradingConfig config, List<FileInfo> testDataFiles);
}
