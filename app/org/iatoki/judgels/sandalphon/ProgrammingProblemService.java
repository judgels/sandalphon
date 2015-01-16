package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ProgrammingProblemService {

    ProgrammingProblem findProblemById(long id);

    void updateProblem(long id, String name, String note);

    Page<ProgrammingProblem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString);

    ProgrammingProblem createProblem(String name, String gradingMethod, String note);

    String getProblemStatement(long id);

    String getProblemGrading(long id);

    void updateProblemStatement(long id, String statement);

    void uploadGradingFile(long id, File file, String filename);

    void updateProblemGrading(long id, String json);

    List<String> getGradingFilenames(long id);

    void submit(String problemJid, Map<String, byte[]> sourceFiles);
}
