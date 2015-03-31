package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.commons.AbstractProblem;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.ProgrammingProblem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

public interface ProgrammingProblemService {

    ProgrammingProblem findProgrammingProblemByJid(String problemJid);

    ProgrammingProblem findProgrammingProblemByJid(String problemJid, AbstractProblem problemPart);

    ProgrammingProblem createProgrammingProblem(String gradingEngine, String additionalNote, LanguageRestriction languageRestriction);

    void updateProgrammingProblem(String problemJid, String gradingEngine, String additionalNote, LanguageRestriction languageRestriction);

    GradingConfig getGradingConfig(String problemJid);

    Date getGradingLastUpdateTime(String problemJid);

    void uploadTestDataFile(String problemJid, File testDataFile, String filename);

    void uploadTestDataFileZipped(String problemJid, File testDataFileZipped);

    void uploadHelperFile(String problemJid, File helperFile, String filename);

    void uploadHelperFileZipped(String problemJid, File helperFileZipped);

    void updateGradingConfig(String problemJid, GradingConfig config);

    List<File> getTestDataFiles(String problemJid);

    List<File> getHelperFiles(String problemJid);

    File getTestDataFile(String problemJid, String filename);

    File getHelperFile(String problemJid, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
