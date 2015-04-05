package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.Page;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ProblemService {

    Problem createProblem(ProblemType type, String name, String additionalNote, String initialLanguageCode);

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    void updateProblem(long problemId, String name, String additionalNote);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String problemJid);

    void addLanguage(String userJid, String problemJid, String languageCode);

    void enableLanguage(String userJid, String problemJid, String languageCode);

    void disableLanguage(String userJid, String problemJid, String languageCode);

    void makeDefaultLanguage(String userJid, String problemJid, String languageCode);

    String getDefaultLanguage(String userJid, String problemJid);

    String getStatement(String userJid, String problemJid, String languageCode);

    void updateStatement(String userJid, long problemId, String languageCode, String statement);

    void uploadStatementMediaFile(String userJid, long problemId, File mediaFile, String filename);

    void uploadStatementMediaFileZipped(String userJid, long problemId, File mediaFileZipped);

    List<FileInfo> getStatementMediaFiles(String userJid, String problemJid);

    String getStatementMediaFileURL(String userJid, String problemJid, String filename);

    List<GitCommit> getVersions(String userJid, String problemJid);

    void initRepository(String userJid, String problemJid);

    boolean userCloneExists(String userJid, String problemJid);

    void createUserCloneIfNotExists(String userJid, String problemJid);

    boolean commitThenMergeUserClone(String userJid, String problemJid, String title, String description);

    boolean updateUserClone(String userJid, String problemJid);

    boolean pushUserClone(String userJid, String problemJid);

    boolean fetchUserClone(String userJid, String problemJid);

    void discardUserClone(String userJid, String problemJid);

    void restore(String problemJid, String hash);
}
