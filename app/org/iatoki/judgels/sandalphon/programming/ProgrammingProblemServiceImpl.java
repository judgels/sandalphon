package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProblemDao problemDao;
    private final FileSystemProvider fileSystemProvider;

    public ProgrammingProblemServiceImpl(ProblemDao problemDao, FileSystemProvider fileSystemProvider) {
        this.problemDao = problemDao;
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public ProgrammingProblem findProgrammingProblemByJid(String problemJid) {
        ProblemModel problemModel = problemDao.findByJid(problemJid);
        return new ProgrammingProblem(problemModel.id, problemJid, problemModel.name, problemModel.userCreate, problemModel.additionalNote, new Date(problemModel.timeUpdate), getGradingEngine(problemJid), getLanguageRestriction(problemJid));
    }

    @Override
    public void createProgrammingProblem(String problemJid) {
        initGrading(problemJid);
    }

    @Override
    public Date getGradingLastUpdateTime(String problemJid) {
        String lastUpdateTime = fileSystemProvider.readFromFile(getGradingLastUpdateTimeFilePath(problemJid));
        return new Date(Long.parseLong(lastUpdateTime));
    }

    @Override
    public GradingConfig getGradingConfig(String problemJid) {
        String gradingEngine = fileSystemProvider.readFromFile(getGradingEngineFilePath(problemJid));
        String gradingConfig = fileSystemProvider.readFromFile(getGradingConfigFilePath(problemJid));

        return GradingEngineRegistry.getInstance().getEngine(gradingEngine).createGradingConfigFromJson(gradingConfig);
    }

    @Override
    public void updateGradingConfig(String problemJid, GradingConfig gradingConfig) {
        fileSystemProvider.writeToFile(getGradingConfigFilePath(problemJid), new Gson().toJson(gradingConfig));

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public String getGradingEngine(String problemJid) {
        return fileSystemProvider.readFromFile(getGradingEngineFilePath(problemJid));
    }

    @Override
    public void updateGradingEngine(String problemJid, String gradingEngine) {
        fileSystemProvider.writeToFile(getGradingEngineFilePath(problemJid), gradingEngine);

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public LanguageRestriction getLanguageRestriction(String problemJid) {
        String languageRestriction = fileSystemProvider.readFromFile(getLanguageRestrictionFilePath(problemJid));
        return new Gson().fromJson(languageRestriction, LanguageRestriction.class);
    }

    @Override
    public void updateLanguageRestriction(String problemJid, LanguageRestriction languageRestriction) {
        fileSystemProvider.writeToFile(getLanguageRestrictionFilePath(problemJid), new Gson().toJson(languageRestriction));

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public void uploadGradingTestDataFile(String problemJid, File testDataFile, String filename) {
        fileSystemProvider.uploadFile(getGradingTestDataDirPath(problemJid), testDataFile, filename);

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public void uploadGradingTestDataFileZipped(String problemJid, File testDataFileZipped) {
        fileSystemProvider.uploadZippedFiles(getGradingTestDataDirPath(problemJid), testDataFileZipped);

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public void uploadGradingHelperFile(String problemJid, File helperFile, String filename) {
        fileSystemProvider.uploadFile(getGradingHelpersDirPath(problemJid), helperFile, filename);

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public void uploadGradingHelperFileZipped(String problemJid, File helperFileZipped) {
        fileSystemProvider.uploadZippedFiles(getGradingHelpersDirPath(problemJid), helperFileZipped);

        updateGradingLastUpdateTime(problemJid);
    }

    @Override
    public List<FileInfo> getGradingTestDataFiles(String problemJid) {
        return fileSystemProvider.listFilesInDirectory(getGradingTestDataDirPath(problemJid));
    }

    @Override
    public List<FileInfo> getGradingHelperFiles(String problemJid) {
        return fileSystemProvider.listFilesInDirectory(getGradingHelpersDirPath(problemJid));
    }

    @Override
    public String getGradingTestDataFileURL(String problemJid, String filename) {
        return fileSystemProvider.getURL(appendPath(getGradingTestDataDirPath(problemJid), filename));
    }


    @Override
    public String getGradingHelperFileURL(String problemJid, String filename) {
        return fileSystemProvider.getURL(appendPath(getGradingHelpersDirPath(problemJid), filename));
    }

    @Override
    public ByteArrayOutputStream getZippedGradingFilesStream(String problemJid) {
        return fileSystemProvider.getZippedFilesInDirectory(getGradingDirPath(problemJid));
    }

    private void updateGradingLastUpdateTime(String problemJid) {
        fileSystemProvider.writeToFile(getGradingLastUpdateTimeFilePath(problemJid), "" + System.currentTimeMillis());
    }

    private void initGrading(String problemJid) {
        fileSystemProvider.createDirectory(getGradingDirPath(problemJid));
        fileSystemProvider.createDirectory(getGradingTestDataDirPath(problemJid));
        fileSystemProvider.createDirectory(getGradingHelpersDirPath(problemJid));

        String engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        fileSystemProvider.writeToFile(getGradingEngineFilePath(problemJid), engine);

        GradingConfig config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
        fileSystemProvider.writeToFile(getGradingConfigFilePath(problemJid), new Gson().toJson(config));

        updateGradingLastUpdateTime(problemJid);
    }

    private ArrayList<String> getRootDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    private ArrayList<String> getGradingDirPath(String problemJid) {
        return appendPath(getRootDirPath(problemJid), "grading");
    }

    private ArrayList<String> getGradingTestDataDirPath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "testdata");
    }

    private ArrayList<String> getGradingHelpersDirPath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "helpers");
    }

    private ArrayList<String> getGradingConfigFilePath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "config.json");
    }

    private ArrayList<String> getGradingEngineFilePath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "engine.txt");
    }

    private ArrayList<String> getLanguageRestrictionFilePath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "languageRestriction.txt");
    }

    private ArrayList<String> getGradingLastUpdateTimeFilePath(String problemJid) {
        return appendPath(getGradingDirPath(problemJid), "lastUpdateTime.txt");
    }

    private ArrayList<String> appendPath(ArrayList<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
