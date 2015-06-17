package org.iatoki.judgels.sandalphon.services.impls;

import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {
    private final FileSystemProvider fileSystemProvider;

    public ProgrammingProblemServiceImpl(FileSystemProvider fileSystemProvider) {
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public void initProgrammingProblem(String problemJid, String gradingEngine) throws IOException {
        fileSystemProvider.createDirectory(getGradingDirPath(null, problemJid));

        fileSystemProvider.createDirectory(getGradingTestDataDirPath(null, problemJid));
        fileSystemProvider.createFile(ProblemServiceUtils.appendPath(getGradingTestDataDirPath(null, problemJid), ".gitkeep"));

        fileSystemProvider.createDirectory(getGradingHelpersDirPath(null, problemJid));
        fileSystemProvider.createFile(ProblemServiceUtils.appendPath(getGradingHelpersDirPath(null, problemJid), ".gitkeep"));

        fileSystemProvider.writeToFile(getGradingEngineFilePath(null, problemJid), gradingEngine);
        fileSystemProvider.writeToFile(getLanguageRestrictionFilePath(null, problemJid), new Gson().toJson(LanguageRestriction.defaultRestriction()));

        GradingConfig config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
        fileSystemProvider.writeToFile(getGradingConfigFilePath(null, problemJid), new Gson().toJson(config));

        updateGradingLastUpdateTime(null, problemJid);
    }

    @Override
    public Date getGradingLastUpdateTime(String userJid, String problemJid) throws IOException {
        String lastUpdateTime = fileSystemProvider.readFromFile(getGradingLastUpdateTimeFilePath(userJid, problemJid));
        return new Date(Long.parseLong(lastUpdateTime));
    }

    @Override
    public GradingConfig getGradingConfig(String userJid, String problemJid) throws IOException {
        String gradingEngine = fileSystemProvider.readFromFile(getGradingEngineFilePath(userJid, problemJid));
        String gradingConfig = fileSystemProvider.readFromFile(getGradingConfigFilePath(userJid, problemJid));

        return GradingEngineRegistry.getInstance().getEngine(gradingEngine).createGradingConfigFromJson(gradingConfig);
    }

    @Override
    public void updateGradingConfig(String userJid, String problemJid, GradingConfig gradingConfig) throws IOException {
        fileSystemProvider.writeToFile(getGradingConfigFilePath(userJid, problemJid), new Gson().toJson(gradingConfig));

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public String getGradingEngine(String userJid, String problemJid) throws IOException {
        return fileSystemProvider.readFromFile(getGradingEngineFilePath(userJid, problemJid));
    }

    @Override
    public void updateGradingEngine(String userJid, String problemJid, String gradingEngine) throws IOException {
        fileSystemProvider.writeToFile(getGradingEngineFilePath(userJid, problemJid), gradingEngine);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public LanguageRestriction getLanguageRestriction(String userJid, String problemJid) throws IOException {
        String languageRestriction = fileSystemProvider.readFromFile(getLanguageRestrictionFilePath(userJid, problemJid));
        return new Gson().fromJson(languageRestriction, LanguageRestriction.class);
    }

    @Override
    public void updateLanguageRestriction(String userJid, String problemJid, LanguageRestriction languageRestriction) throws IOException {
        fileSystemProvider.writeToFile(getLanguageRestrictionFilePath(userJid, problemJid), new Gson().toJson(languageRestriction));

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingTestDataFile(String userJid, String problemJid, File testDataFile, String filename) throws IOException {
        fileSystemProvider.uploadFile(getGradingTestDataDirPath(userJid, problemJid), testDataFile, filename);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingTestDataFileZipped(String userJid, String problemJid, File testDataFileZipped) throws IOException {
        fileSystemProvider.uploadZippedFiles(getGradingTestDataDirPath(userJid, problemJid), testDataFileZipped, false);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingHelperFile(String userJid, String problemJid, File helperFile, String filename) throws IOException {
        fileSystemProvider.uploadFile(getGradingHelpersDirPath(userJid, problemJid), helperFile, filename);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingHelperFileZipped(String userJid, String problemJid, File helperFileZipped) throws IOException {
        fileSystemProvider.uploadZippedFiles(getGradingHelpersDirPath(userJid, problemJid), helperFileZipped, false);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public List<FileInfo> getGradingTestDataFiles(String userJid, String problemJid) {
        return fileSystemProvider.listFilesInDirectory(getGradingTestDataDirPath(userJid, problemJid));
    }

    @Override
    public List<FileInfo> getGradingHelperFiles(String userJid, String problemJid) {
        return fileSystemProvider.listFilesInDirectory(getGradingHelpersDirPath(userJid, problemJid));
    }

    @Override
    public String getGradingTestDataFileURL(String userJid, String problemJid, String filename) {
        return fileSystemProvider.getURL(ProblemServiceUtils.appendPath(getGradingTestDataDirPath(userJid, problemJid), filename));
    }


    @Override
    public String getGradingHelperFileURL(String userJid, String problemJid, String filename) {
        return fileSystemProvider.getURL(ProblemServiceUtils.appendPath(getGradingHelpersDirPath(userJid, problemJid), filename));
    }

    @Override
    public ByteArrayOutputStream getZippedGradingFilesStream(String problemJid) throws IOException {
        return fileSystemProvider.getZippedFilesInDirectory(getGradingDirPath(null, problemJid));
    }

    private void updateGradingLastUpdateTime(String userJid, String problemJid) throws IOException {
        fileSystemProvider.writeToFile(getGradingLastUpdateTimeFilePath(userJid, problemJid), "" + System.currentTimeMillis());
    }

    private List<String> getGradingDirPath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(ProblemServiceUtils.getRootDirPath(fileSystemProvider, userJid, problemJid), "grading");
    }

    private List<String> getGradingTestDataDirPath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "testdata");
    }

    private List<String> getGradingHelpersDirPath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "helpers");
    }

    private List<String> getGradingConfigFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "config.json");
    }

    private List<String> getGradingEngineFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "engine.txt");
    }

    private List<String> getLanguageRestrictionFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "languageRestriction.txt");
    }

    private List<String> getGradingLastUpdateTimeFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getGradingDirPath(userJid, problemJid), "lastUpdateTime.txt");
    }
}
