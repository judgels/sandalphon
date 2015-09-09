package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonPartner;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel;

import java.util.Date;
import java.util.List;

public final class LessonServiceUtils {

    private LessonServiceUtils() {
        // prevent instantiation
    }

    static List<String> getOriginDirPath(String lessonJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseLessonsDirKey(), lessonJid);
    }

    static List<String> getClonesDirPath(String lessonJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseLessonClonesDirKey(), lessonJid);
    }

    static List<String> getCloneDirPath(String userJid, String lessonJid) {
        return appendPath(getClonesDirPath(lessonJid), userJid);
    }

    static List<String> getRootDirPath(FileSystemProvider fileSystemProvider, String userJid, String lessonJid) {
        List<String> origin =  getOriginDirPath(lessonJid);
        List<String> root = getCloneDirPath(userJid, lessonJid);

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

    static Lesson createLessonFromModel(LessonModel lessonModel) {
        return new Lesson(lessonModel.id, lessonModel.jid, lessonModel.slug, lessonModel.userCreate, lessonModel.additionalNote, new Date(lessonModel.timeUpdate));
    }

    static LessonPartner createLessonPartnerFromModel(LessonPartnerModel lessonPartnerModel) {
        return new LessonPartner(lessonPartnerModel.id, lessonPartnerModel.lessonJid, lessonPartnerModel.userJid, lessonPartnerModel.config);
    }
}
