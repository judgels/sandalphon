package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.gabriel.GradingType;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ProblemService {

    boolean isProblemExistByProblemJid(String problemJid);

    Problem findProblemById(long id);

    void updateProblem(long id, String name, String additionalNote);

    Page<Problem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString);

    Problem createProblem(String name, String gradingType, String additionalNote);

    String getStatement(long id);

    String getStatement(String problemJid);

    GradingConfig getGradingConfig(long id);

    void updateStatement(long id, String statement);

    void uploadTestDataFile(long id, File file, String filename);

    void updateGradingConfig(long id, GradingConfig config);

    List<File> getTestDataFiles(long id);

    File getTestDataFile(long id, String filename);

    List<File> getHelperFiles(long id);

    List<File> getMediaFiles(long id);
}
