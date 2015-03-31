package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.commons.Problem;

import java.io.File;
import java.util.List;

public interface ProblemService {

    Problem createProblem(String name, String childJid);

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    void updateProblem(long problemId, String name);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void updateStatement(long problemId, String statement);

    String getStatement(String problemJid);

    void uploadStatementMediaFile(long problemId, File mediaFile, String filename);

    void uploadStatementMediaFileZipped(long problemId, File mediaFileZipped);

    List<File> getStatementMediaFiles(String problemJid);

    File getStatementMediaFile(String problemJid, String filename);
}
