package org.iatoki.judgels.sandalphon.services.impls;

import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.sandalphon.bundle.BundleItemUtils;
import org.iatoki.judgels.sandalphon.bundle.BundleItemsConfig;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;

import java.io.IOException;

public final class BundleProblemServiceImpl implements BundleProblemService {
    private final FileSystemProvider fileSystemProvider;

    public BundleProblemServiceImpl(FileSystemProvider fileSystemProvider) {
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public void initBundleProblem(String problemJid) throws IOException {
        fileSystemProvider.createDirectory(BundleServiceUtils.getItemsDirPath(fileSystemProvider, problemJid, null));

        BundleItemsConfig config = BundleItemUtils.createDefaultItemConfig();
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, null), new Gson().toJson(config));
    }
}
