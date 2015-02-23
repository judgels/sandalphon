package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.commons.Submission;

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

    String getStatement(long problemId);

    String getStatement(String problemJid);

    GradingConfig getGradingConfig(long problemId);

    Date getGradingLastUpdateTime(long problemId);

    void updateStatement(long problemId, String statement);

    void uploadTestDataFile(long problemId, File testDataFile, String filename);

    void uploadTestDataFileZipped(long problemId, File testDataFileZipped);

    void uploadHelperFile(long problemId, File helperFile, String filename);

    void uploadHelperFileZipped(long problemId, File helperFileZipped);

    void uploadMediaFile(long problemId, File mediaFile, String filename);

    void uploadMediaFileZipped(long problemId, File mediaFileZipped);

    void updateGradingConfig(long problemId, GradingConfig config);

    List<File> getTestDataFiles(long problemId);

    List<File> getHelperFiles(long problemId);

    List<File> getMediaFiles(long problemId);

    File getTestDataFile(long problemId, String filename);

    File getHelperFile(long problemId, String filename);

    File getMediaFile(long problemId, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
