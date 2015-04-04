package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.FileInfo;
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

    Map<String, StatementLanguageStatus> getAvailableLanguages(String problemJid);

    void addLanguage(String problemJid, String languageCode);

    void enableLanguage(String problemJid, String languageCode);

    void disableLanguage(String problemJid, String languageCode);

    void makeDefaultLanguage(String problemJid, String languageCode);

    String getDefaultLanguage(String problemJid);

    String getStatement(String problemJid, String languageCode);

    void updateStatement(long problemId, String languageCode, String statement);

    void uploadStatementMediaFile(long problemId, File mediaFile, String filename);

    void uploadStatementMediaFileZipped(long problemId, File mediaFileZipped);

    List<FileInfo> getStatementMediaFiles(String problemJid);

    String getStatementMediaFileURL(String problemJid, String filename);
}
