package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

public interface ProblemService {

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    void updateProblem(long problemId, String name, String gradingEngine, String additionalNote);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    Problem createProblem(String name, String gradingEngine, String additionalNote);

    String getStatement(String problemJid);

    GradingConfig getGradingConfig(String problemJid);

    Date getGradingLastUpdateTime(String problemJid);

    void updateStatement(long problemId, String statement);

    void uploadTestDataFile(long problemId, File testDataFile, String filename);

    void uploadTestDataFileZipped(long problemId, File testDataFileZipped);

    void uploadHelperFile(long problemId, File helperFile, String filename);

    void uploadHelperFileZipped(long problemId, File helperFileZipped);

    void uploadMediaFile(long problemId, File mediaFile, String filename);

    void uploadMediaFileZipped(long problemId, File mediaFileZipped);

    void updateGradingConfig(long problemId, GradingConfig config);

    List<File> getTestDataFiles(String problemJid);

    List<File> getHelperFiles(String problemJid);

    List<File> getMediaFiles(String problemJid);

    File getTestDataFile(String problemJid, String filename);

    File getHelperFile(String problemJid, String filename);

    File getMediaFile(String problemJid, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
