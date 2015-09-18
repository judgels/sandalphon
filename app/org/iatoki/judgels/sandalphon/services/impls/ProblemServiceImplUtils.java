package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.sandalphon.SandalphonProperties;

import java.util.List;

final class ProblemServiceImplUtils {

    private ProblemServiceImplUtils() {
        // prevent instantiation
    }

    static List<String> getOriginDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    static List<String> getClonesDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemClonesDirKey(), problemJid);
    }

    static List<String> getCloneDirPath(String userJid, String problemJid) {
        return appendPath(getClonesDirPath(problemJid), userJid);
    }

    static List<String> getRootDirPath(FileSystemProvider fileSystemProvider, String userJid, String problemJid) {
        List<String> origin =  getOriginDirPath(problemJid);
        List<String> root = getCloneDirPath(userJid, problemJid);

        if (userJid == null || !fileSystemProvider.directoryExists(root)) {
            return origin;
        } else {
            return root;
        }
    }

    static List<String> appendPath(List<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
