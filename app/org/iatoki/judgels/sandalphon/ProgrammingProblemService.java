package org.iatoki.judgels.sandalphon;

import java.io.File;
import java.util.List;

public interface ProgrammingProblemService {

    Problem createProblem(String name, String note);

    String getProblemStatement(long id);

    void updateProblemStatement(long id, String statement);

    void uploadGradingFile(long id, File file, String filename);

    void updateProblemGrading(long id, int timeLimit, int memoryLimit, List<List<String>> tcIn, List<List<String>> tcOut, List<List<Integer>> subtaskBatches, List<Integer> subtaskPoints);

    List<String> getGradingFilenames(long id);
}
