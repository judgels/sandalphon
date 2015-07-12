package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.sandalphon.SandalphonProperties;

import java.util.List;

public final class LessonServiceUtils {

    public static List<String> getOriginDirPath(String lessonJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseLessonsDirKey(), lessonJid);
    }

    public static List<String> getClonesDirPath(String lessonJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseLessonClonesDirKey(), lessonJid);
    }

    public static List<String> getCloneDirPath(String userJid, String lessonJid) {
        return appendPath(getClonesDirPath(lessonJid), userJid);
    }

    public static List<String> getRootDirPath(FileSystemProvider fileSystemProvider, String userJid, String lessonJid) {
        List<String> origin =  getOriginDirPath(lessonJid);
        List<String> root = getCloneDirPath(userJid, lessonJid);

        if (userJid == null || !fileSystemProvider.directoryExists(root)) {
            return origin;
        } else {
            return root;
        }
    }

    public static List<String> appendPath(List<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
