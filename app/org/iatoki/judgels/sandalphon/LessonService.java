package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.Page;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface LessonService {

    Lesson createLesson(String name, String additionalNote, String initialLanguageCode) throws IOException;

    boolean lessonExistsByJid(String lessonJid);

    Lesson findLessonById(long lessonId) throws LessonNotFoundException;

    Lesson findLessonByJid(String lessonJid);

    boolean isLessonPartnerByUserJid(String lessonJid, String userJid);

    void createLessonPartner(long lessonId, String userJid, LessonPartnerConfig config);

    void updateLessonPartner(long lessonPartnerId, LessonPartnerConfig config);

    Page<LessonPartner> pageLessonPartners(String lessonJid, long pageIndex, long pageSize, String orderBy, String orderDir);

    LessonPartner findLessonPartnerByLessonPartnerId(long lessonPartnerId) throws LessonPartnerNotFoundException;

    LessonPartner findLessonPartnerByLessonJidAndPartnerJid(String lessonJid, String partnerJid);

    void updateLesson(long lessonId, String name, String additionalNote);

    Page<Lesson> pageLessons(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin);

    Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String lessonJid) throws IOException;

    void addLanguage(String userJid, String lessonJid, String languageCode) throws IOException;

    void enableLanguage(String userJid, String lessonJid, String languageCode) throws IOException;

    void disableLanguage(String userJid, String lessonJid, String languageCode) throws IOException;

    void makeDefaultLanguage(String userJid, String lessonJid, String languageCode) throws IOException;

    String getDefaultLanguage(String userJid, String lessonJid) throws IOException;

    String getStatement(String userJid, String lessonJid, String languageCode) throws IOException;

    void updateStatement(String userJid, long lessonId, String languageCode, String statement) throws IOException;

    void uploadStatementMediaFile(String userJid, long lessonId, File mediaFile, String filename) throws IOException;

    void uploadStatementMediaFileZipped(String userJid, long lessonId, File mediaFileZipped) throws IOException;

    List<FileInfo> getStatementMediaFiles(String userJid, String lessonJid);

    String getStatementMediaFileURL(String userJid, String lessonJid, String filename);

    List<GitCommit> getVersions(String userJid, String lessonJid);

    void initRepository(String userJid, String lessonJid);

    boolean userCloneExists(String userJid, String lessonJid);

    void createUserCloneIfNotExists(String userJid, String lessonJid);

    boolean commitThenMergeUserClone(String userJid, String lessonJid, String title, String description);

    boolean updateUserClone(String userJid, String lessonJid);

    boolean pushUserClone(String userJid, String lessonJid);

    boolean fetchUserClone(String userJid, String lessonJid);

    void discardUserClone(String userJid, String lessonJid) throws IOException;

    void restore(String lessonJid, String hash);
}
