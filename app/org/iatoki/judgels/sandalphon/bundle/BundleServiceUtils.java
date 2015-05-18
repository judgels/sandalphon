package org.iatoki.judgels.sandalphon.bundle;

import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.sandalphon.ProblemServiceUtils;

import java.util.List;

public final class BundleServiceUtils {

    private BundleServiceUtils() {
        // prevent instantiation
    }

    static List<String> getItemsDirPath(FileSystemProvider fileSystemProvider, String problemJid, String userJid) {
        return ProblemServiceUtils.appendPath(ProblemServiceUtils.getRootDirPath(fileSystemProvider, userJid, problemJid), "items");
    }

    static List<String> getItemsConfigFilePath(FileSystemProvider fileSystemProvider, String problemJid, String userJid) {
        return ProblemServiceUtils.appendPath(getItemsDirPath(fileSystemProvider, problemJid, userJid), "items.json");
    }

    static List<String> getItemDirPath(FileSystemProvider fileSystemProvider, String problemJid, String userJid, String itemJid) {
        return ProblemServiceUtils.appendPath(getItemsDirPath(fileSystemProvider, problemJid, userJid), itemJid);
    }

    static List<String> getItemConfigFilePath(FileSystemProvider fileSystemProvider, String problemJid, String userJid, String itemJid, String languageCode) {
        return ProblemServiceUtils.appendPath(getItemDirPath(fileSystemProvider, problemJid, userJid, itemJid), languageCode + ".json");
    }

}
