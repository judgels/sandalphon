package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.Page;

import java.io.File;
import java.util.List;

public interface ProblemService {

    Problem createProblem(ProblemType type, String name, String additionalNote);

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    void updateProblem(long problemId, String name, String additionalNote);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    String getStatement(String problemJid);

    void updateStatement(long problemId, String statement);

    void uploadStatementMediaFile(long problemId, File mediaFile, String filename);

    void uploadStatementMediaFileZipped(long problemId, File mediaFileZipped);

    List<FileInfo> getStatementMediaFiles(String problemJid);

    String getStatementMediaFileURL(String problemJid, String filename);
}
