package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.ProgrammingProblem;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProgrammingProblemModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProgrammingProblemDao problemDao;

    public ProgrammingProblemServiceImpl(ProgrammingProblemDao problemDao) {
        this.problemDao = problemDao;
    }

    @Override
    public ProgrammingProblem findProgrammingProblemByJid(String problemJid) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        return createProgrammingProblemFromModel(problemModel);
    }

    @Override
    public ProgrammingProblem findProgrammingProblemByJid(String problemJid, Problem problemPart) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        return createProgrammingProblemFromModel(problemModel, problemPart);
    }

    @Override
    public ProgrammingProblem createProgrammingProblem(String gradingEngine, String additionalNote, LanguageRestriction languageRestriction) {
        ProgrammingProblemModel problemModel = new ProgrammingProblemModel();
        problemModel.gradingEngine = gradingEngine;
        problemModel.additionalNote = additionalNote;
        problemModel.languageRestriction = new Gson().toJson(languageRestriction);

        problemDao.persist(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initGrading(problemModel);

        return createProgrammingProblemFromModel(problemModel);
    }

    @Override
    public void updateProgrammingProblem(String problemJid, String gradingEngine, String additionalNote, LanguageRestriction languageRestriction) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);

        if (!gradingEngine.equals(problemModel.gradingEngine)) {
            updateGradingConfig(problemJid, GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig());
        }

        problemModel.gradingEngine = gradingEngine;
        problemModel.additionalNote = additionalNote;
        problemModel.languageRestriction = new Gson().toJson(languageRestriction);

        updateProblemModel(problemModel);
    }

    @Override
    public GradingConfig getGradingConfig(String problemJid) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File gradingDir = SandalphonProperties.getInstance().getProgrammingGradingDir(problemJid);
        String json;
        try {
            json = FileUtils.readFileToString(new File(gradingDir, "config.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return GradingEngineRegistry.getInstance().getEngine(problemModel.gradingEngine).createGradingConfigFromJson(json);
    }

    @Override
    public Date getGradingLastUpdateTime(String problemJid) {
        File lastUpdateTimeFile = new File(SandalphonProperties.getInstance().getProgrammingGradingDir(problemJid), "lastUpdateTime.txt");
        try {
            return new Date(Long.parseLong(FileUtils.readFileToString(lastUpdateTimeFile)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void uploadTestDataFile(String problemJid, File testDataFile, String filename) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File testDataDir = SandalphonProperties.getInstance().getProgrammingGradingTestDataDir(problemJid);
        try {
            FileUtils.copyFile(testDataFile, new File(testDataDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel);
    }

    @Override
    public void uploadTestDataFileZipped(String problemJid, File testDataFileZipped) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File testDataDir = SandalphonProperties.getInstance().getProgrammingGradingTestDataDir(problemJid);

        SandalphonUtils.uploadZippedFiles(testDataDir, testDataFileZipped);

        updateProblemModel(problemModel);
    }

    @Override
    public void uploadHelperFile(String problemJid, File helperFile, String filename) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File helperDir = SandalphonProperties.getInstance().getProgrammingGradingHelperDir(problemJid);
        try {
            FileUtils.copyFile(helperFile, new File(helperDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel);
    }

    @Override
    public void uploadHelperFileZipped(String problemJid, File testDataFileZipped) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File helperDir = SandalphonProperties.getInstance().getProgrammingGradingHelperDir(problemJid);

        SandalphonUtils.uploadZippedFiles(helperDir, testDataFileZipped);

        updateProblemModel(problemModel);
    }

    @Override
    public void updateGradingConfig(String problemJid, GradingConfig config) {
        ProgrammingProblemModel problemModel = problemDao.findByJid(problemJid);
        File gradingDir = SandalphonProperties.getInstance().getProgrammingGradingDir(problemJid);
        try {
            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel);
    }

    @Override
    public List<File> getTestDataFiles(String problemJid) {
        File testDataDir = SandalphonProperties.getInstance().getProgrammingGradingTestDataDir(problemJid);
        return SandalphonUtils.getSortedFilesInDir(testDataDir);
    }

    @Override
    public List<File> getHelperFiles(String problemJid) {
        File helperDir = SandalphonProperties.getInstance().getProgrammingGradingHelperDir(problemJid);
        return SandalphonUtils.getSortedFilesInDir(helperDir);
    }

    @Override
    public File getTestDataFile(String problemJid, String filename) {
        File testDataDir = SandalphonProperties.getInstance().getProgrammingGradingTestDataDir(problemJid);
        return new File(testDataDir, filename);
    }


    @Override
    public File getHelperFile(String problemJid, String filename) {
        File helperDir = SandalphonProperties.getInstance().getProgrammingGradingHelperDir(problemJid);
        return new File(helperDir, filename);
    }

    @Override
    public ByteArrayOutputStream getZippedGradingFilesStream(String problemJid) {
        List<File> files = getGradingFiles(problemJid);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        try {
            ZipOutputStream zos = new ZipOutputStream(os);
            for (File file : files) {
                int beginIndex = file.getAbsolutePath().indexOf(problemJid) + problemJid.length() + 1 + "grading".length() + 1;
                ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(beginIndex));
                zos.putNextEntry(ze);

                try (FileInputStream fin = new FileInputStream(file)) {
                    int len;
                    while ((len = fin.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
            }

            zos.closeEntry();
            zos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return os;
    }

    private List<File> getGradingFiles(String problemJid) {
        File gradingDir = SandalphonProperties.getInstance().getProgrammingGradingDir(problemJid);

        ImmutableList.Builder<File> files = ImmutableList.builder();
        populateGradingFiles(gradingDir, files);
        return files.build();
    }

    private void populateGradingFiles(File node, ImmutableList.Builder<File> files) {
        if (node.isFile()) {
            files.add(node);
        } else {
            File[] newNodes = node.listFiles();
            if (newNodes != null) {
                for (File newNode : newNodes) {
                    populateGradingFiles(newNode, files);
                }
            }
        }
    }

    private void updateProblemModel(ProgrammingProblemModel problemModel) {
        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        File lastUpdateTimeFile = new File(SandalphonProperties.getInstance().getProgrammingGradingDir(problemModel.jid), "lastUpdateTime.txt");

        try {
            FileUtils.forceDelete(lastUpdateTimeFile);
            FileUtils.writeStringToFile(lastUpdateTimeFile, "" + problemModel.timeUpdate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProgrammingProblem createProgrammingProblemFromModel(ProgrammingProblemModel problemModel) {
        return new ProgrammingProblem(problemModel.jid, problemModel.gradingEngine, problemModel.additionalNote, new Gson().fromJson(problemModel.languageRestriction, LanguageRestriction.class));
    }

    private ProgrammingProblem createProgrammingProblemFromModel(ProgrammingProblemModel problemModel, Problem problemPart) {
        return new ProgrammingProblem(problemPart.getId(), problemModel.jid, problemPart.getName(), problemPart.getAuthorJid(), problemPart.getLastUpdate(), problemModel.gradingEngine, problemModel.additionalNote, new Gson().fromJson(problemModel.languageRestriction, LanguageRestriction.class));
    }

    private void initGrading(ProgrammingProblemModel problemModel) {
        File gradingDir = SandalphonProperties.getInstance().getProgrammingGradingDir(problemModel.jid);
        File testDataDir = SandalphonProperties.getInstance().getProgrammingGradingTestDataDir(problemModel.jid);
        File helperDir = SandalphonProperties.getInstance().getProgrammingGradingHelperDir(problemModel.jid);

        try {
            FileUtils.forceMkdir(gradingDir);
            FileUtils.forceMkdir(testDataDir);
            FileUtils.forceMkdir(helperDir);

            GradingConfig config = GradingEngineRegistry.getInstance().getEngine(problemModel.gradingEngine).createDefaultGradingConfig();

            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
            FileUtils.writeStringToFile(new File(gradingDir, "lastUpdateTime.txt"), "" + problemModel.timeCreate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
