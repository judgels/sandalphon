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

    GradingConfig getGradingConfig(String userJid, String problemJid);

    void updateGradingConfig(String userJid, String problemJid, GradingConfig gradingConfig);

    Date getGradingLastUpdateTime(String userJid, String problemJid);

    String getGradingEngine(String userJid, String problemJid);

    void updateGradingEngine(String userJid, String problemJid, String gradingEngine);

    LanguageRestriction getLanguageRestriction(String userJid, String problemJid);

    void updateLanguageRestriction(String userJid, String problemJid, LanguageRestriction languageRestriction);

    void uploadGradingTestDataFile(String userJid, String problemJid, File testDataFile, String filename);

    void uploadGradingTestDataFileZipped(String userJid, String problemJid, File testDataFileZipped);

    void uploadGradingHelperFile(String userJid, String problemJid, File helperFile, String filename);

    void uploadGradingHelperFileZipped(String userJid, String problemJid, File helperFileZipped);

    List<FileInfo> getGradingTestDataFiles(String userJid, String problemJid);

    List<FileInfo> getGradingHelperFiles(String userJid, String problemJid);

    String getGradingTestDataFileURL(String userJid, String problemJid, String filename);

    String getGradingHelperFileURL(String userJid, String problemJid, String filename);

    ByteArrayOutputStream getZippedGradingFilesStream(String problemJid);
}
