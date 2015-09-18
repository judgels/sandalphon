package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.FileSystemProvider;

import java.util.List;

public final class BundleProblemServiceImplUtils {

    private BundleProblemServiceImplUtils() {
        // prevent instantiation
    }

    static List<String> getItemsDirPath(FileSystemProvider fileSystemProvider, String problemJid, String userJid) {
        return ProblemServiceImplUtils.appendPath(ProblemServiceImplUtils.getRootDirPath(fileSystemProvider, userJid, problemJid), "items");
    }

    static List<String> getItemsConfigFilePath(FileSystemProvider fileSystemProvider, String problemJid, String userJid) {
        return ProblemServiceImplUtils.appendPath(getItemsDirPath(fileSystemProvider, problemJid, userJid), "items.json");
    }

    static List<String> getItemDirPath(FileSystemProvider fileSystemProvider, String problemJid, String userJid, String itemJid) {
        return ProblemServiceImplUtils.appendPath(getItemsDirPath(fileSystemProvider, problemJid, userJid), itemJid);
    }

    static List<String> getItemConfigFilePath(FileSystemProvider fileSystemProvider, String problemJid, String userJid, String itemJid, String languageCode) {
        return ProblemServiceImplUtils.appendPath(getItemDirPath(fileSystemProvider, problemJid, userJid, itemJid), languageCode + ".json");
    }
}
