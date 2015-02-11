package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

public interface ProblemService {

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    void updateProblem(long id, String name, String additionalNote);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    Problem createProblem(String name, String gradingType, String additionalNote);

    String getStatement(long id);

    String getStatement(String problemJid);

    GradingConfig getGradingConfig(long id);

    long getGradingLastUpdateTime(long id);

    void updateStatement(long id, String statement);

    void uploadTestDataFile(long id, File testDataFile, String filename);

    void uploadTestDataFileZipped(long id, File testDataFileZipped);

    void uploadHelperFile(long id, File helperFile, String filename);

    void uploadHelperFileZipped(long id, File helperFileZipped);

    void uploadMediaFile(long id, File mediaFile, String filename);

    void uploadMediaFileZipped(long id, File mediaFileZipped);

    void updateGradingConfig(long id, GradingConfig config);

    List<File> getTestDataFiles(long id);

    List<File> getHelperFiles(long id);

    List<File> getMediaFiles(long id);

    File getTestDataFile(long id, String filename);

    File getHelperFile(long id, String filename);

    File getMediaFile(long id, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
