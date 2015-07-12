package org.iatoki.judgels.sandalphon.services.impls;

import com.google.gson.Gson;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemUtils;
import org.iatoki.judgels.sandalphon.BundleItemsConfig;
import org.iatoki.judgels.sandalphon.config.ProblemFile;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Named("bundleProblemService")
public final class BundleProblemServiceImpl implements BundleProblemService {

    private final FileSystemProvider problemFileSystemProvider;

    @Inject
    public BundleProblemServiceImpl(@ProblemFile FileSystemProvider problemFileSystemProvider) {
        this.problemFileSystemProvider = problemFileSystemProvider;
    }

    @Override
    public void initBundleProblem(String problemJid) throws IOException {
        problemFileSystemProvider.createDirectory(BundleServiceUtils.getItemsDirPath(problemFileSystemProvider, problemJid, null));

        BundleItemsConfig config = BundleItemUtils.createDefaultItemConfig();
        problemFileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(problemFileSystemProvider, problemJid, null), new Gson().toJson(config));
    }
}
