package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

public interface ProgrammingProblemService {

    void initProgrammingProblem(String problemJid, String gradingEngine);

    GradingConfig getGradingConfig(String problemJid);

    void updateGradingConfig(String problemJid, GradingConfig gradingConfig);

    Date getGradingLastUpdateTime(String problemJid);

    String getGradingEngine(String problemJid);

    void updateGradingEngine(String problemJid, String gradingEngine);

    LanguageRestriction getLanguageRestriction(String problemJid);

    void updateLanguageRestriction(String problemJid, LanguageRestriction languageRestriction);

    void uploadGradingTestDataFile(String problemJid, File testDataFile, String filename);

    void uploadGradingTestDataFileZipped(String problemJid, File testDataFileZipped);

    void uploadGradingHelperFile(String problemJid, File helperFile, String filename);

    void uploadGradingHelperFileZipped(String problemJid, File helperFileZipped);

    List<FileInfo> getGradingTestDataFiles(String problemJid);

    List<FileInfo> getGradingHelperFiles(String problemJid);

    String getGradingTestDataFileURL(String problemJid, String filename);

    String getGradingHelperFileURL(String problemJid, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
