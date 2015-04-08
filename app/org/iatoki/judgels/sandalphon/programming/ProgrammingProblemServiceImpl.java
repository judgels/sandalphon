package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {
    private final FileSystemProvider fileSystemProvider;

    public ProgrammingProblemServiceImpl(FileSystemProvider fileSystemProvider) {
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public void initProgrammingProblem(String problemJid, String gradingEngine) {
        fileSystemProvider.createDirectory(getGradingDirPath(null, problemJid));

        fileSystemProvider.createDirectory(getGradingTestDataDirPath(null, problemJid));
        fileSystemProvider.createFile(appendPath(getGradingTestDataDirPath(null, problemJid), ".gitkeep"));

        fileSystemProvider.createDirectory(getGradingHelpersDirPath(null, problemJid));
        fileSystemProvider.createFile(appendPath(getGradingHelpersDirPath(null, problemJid), ".gitkeep"));

        fileSystemProvider.writeToFile(getGradingEngineFilePath(null, problemJid), gradingEngine);
        fileSystemProvider.writeToFile(getLanguageRestrictionFilePath(null, problemJid), new Gson().toJson(LanguageRestriction.defaultRestriction()));

        GradingConfig config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
        fileSystemProvider.writeToFile(getGradingConfigFilePath(null, problemJid), new Gson().toJson(config));

        updateGradingLastUpdateTime(null, problemJid);
    }

    @Override
    public Date getGradingLastUpdateTime(String userJid, String problemJid) {
        String lastUpdateTime = fileSystemProvider.readFromFile(getGradingLastUpdateTimeFilePath(userJid, problemJid));
        return new Date(Long.parseLong(lastUpdateTime));
    }

    @Override
    public GradingConfig getGradingConfig(String userJid, String problemJid) {
        String gradingEngine = fileSystemProvider.readFromFile(getGradingEngineFilePath(userJid, problemJid));
        String gradingConfig = fileSystemProvider.readFromFile(getGradingConfigFilePath(userJid, problemJid));

        return GradingEngineRegistry.getInstance().getEngine(gradingEngine).createGradingConfigFromJson(gradingConfig);
    }

    @Override
    public void updateGradingConfig(String userJid, String problemJid, GradingConfig gradingConfig) {
        fileSystemProvider.writeToFile(getGradingConfigFilePath(userJid, problemJid), new Gson().toJson(gradingConfig));

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public String getGradingEngine(String userJid, String problemJid) {
        return fileSystemProvider.readFromFile(getGradingEngineFilePath(userJid, problemJid));
    }

    @Override
    public void updateGradingEngine(String userJid, String problemJid, String gradingEngine) {
        fileSystemProvider.writeToFile(getGradingEngineFilePath(userJid, problemJid), gradingEngine);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public LanguageRestriction getLanguageRestriction(String userJid, String problemJid) {
        String languageRestriction = fileSystemProvider.readFromFile(getLanguageRestrictionFilePath(userJid, problemJid));
        return new Gson().fromJson(languageRestriction, LanguageRestriction.class);
    }

    @Override
    public void updateLanguageRestriction(String userJid, String problemJid, LanguageRestriction languageRestriction) {
        fileSystemProvider.writeToFile(getLanguageRestrictionFilePath(userJid, problemJid), new Gson().toJson(languageRestriction));

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingTestDataFile(String userJid, String problemJid, File testDataFile, String filename) {
        fileSystemProvider.uploadFile(getGradingTestDataDirPath(userJid, problemJid), testDataFile, filename);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingTestDataFileZipped(String userJid, String problemJid, File testDataFileZipped) {
        fileSystemProvider.uploadZippedFiles(getGradingTestDataDirPath(userJid, problemJid), testDataFileZipped);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingHelperFile(String userJid, String problemJid, File helperFile, String filename) {
        fileSystemProvider.uploadFile(getGradingHelpersDirPath(userJid, problemJid), helperFile, filename);

        updateGradingLastUpdateTime(userJid, problemJid);
    }

    @Override
    public void uploadGradingHelperFileZipped(String userJid, String problemJid, File helperFileZipped) {
        fileSystemProvider.uploadZippedFiles(getGradingHelpersDirPath(userJid, problemJid), helperFileZipped);

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
        return fileSystemProvider.getURL(appendPath(getGradingTestDataDirPath(userJid, problemJid), filename));
    }


    @Override
    public String getGradingHelperFileURL(String userJid, String problemJid, String filename) {
        return fileSystemProvider.getURL(appendPath(getGradingHelpersDirPath(userJid, problemJid), filename));
    }

    @Override
    public ByteArrayOutputStream getZippedGradingFilesStream(String problemJid) {
        return fileSystemProvider.getZippedFilesInDirectory(getGradingDirPath(null, problemJid));
    }

    private void updateGradingLastUpdateTime(String userJid, String problemJid) {
        fileSystemProvider.writeToFile(getGradingLastUpdateTimeFilePath(userJid, problemJid), "" + System.currentTimeMillis());
    }

    private ArrayList<String> getOriginDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    private ArrayList<String> getClonesDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemClonesDirKey(), problemJid);
    }

    private ArrayList<String> getCloneDirPath(String userJid, String problemJid) {
        return appendPath(getClonesDirPath(problemJid), userJid);
    }

    private ArrayList<String> getRootDirPath(String userJid, String problemJid) {
        ArrayList<String> origin =  getOriginDirPath(problemJid);
        ArrayList<String> root = getCloneDirPath(userJid, problemJid);

        if (userJid == null || !fileSystemProvider.fileExists(root)) {
            return origin;
        } else {
            return root;
        }
    }

    private ArrayList<String> getGradingDirPath(String userJid, String problemJid) {
        return appendPath(getRootDirPath(userJid, problemJid), "grading");
    }

    private ArrayList<String> getGradingTestDataDirPath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "testdata");
    }

    private ArrayList<String> getGradingHelpersDirPath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "helpers");
    }

    private ArrayList<String> getGradingConfigFilePath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "config.json");
    }

    private ArrayList<String> getGradingEngineFilePath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "engine.txt");
    }

    private ArrayList<String> getLanguageRestrictionFilePath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "languageRestriction.txt");
    }

    private ArrayList<String> getGradingLastUpdateTimeFilePath(String userJid, String problemJid) {
        return appendPath(getGradingDirPath(userJid, problemJid), "lastUpdateTime.txt");
    }

    private ArrayList<String> appendPath(ArrayList<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
