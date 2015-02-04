package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GraderRegistry;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;

    public ProblemServiceImpl(ProblemDao dao) {
        this.dao = dao;
    }

    @Override
    public boolean isProblemExistByProblemJid(String problemJid) {
        return dao.isProblemExistByProblemJid(problemJid);
    }

    @Override
    public final Problem findProblemById(long id) {
        ProblemModel problemRecord = dao.findById(id);
        return createProblemFromModel(problemRecord);
    }

    @Override
    public final void updateProblem(long id, String name, String additionalNote) {
        ProblemModel model = dao.findById(id);
        model.name = name;
        model.additionalNote = additionalNote;
        updateProblemRecord(model, false);
    }

    @Override
    public Page<Problem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = dao.countByFilter(filterString);
        List<ProblemModel> problemRecords = dao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Problem> problems = problemRecords
                .stream()
                .map(problemRecord -> createProblemFromModel(problemRecord))
                .collect(Collectors.toList());

        return new Page<>(problems, totalPage, page, pageSize);
    }

    @Override
    public Problem createProblem(String name, String gradingType, String additionalNote) {
        ProblemModel problemRecord = new ProblemModel(name, gradingType, additionalNote);
        dao.persist(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        createProblemDirs(problemRecord);

        return createProblemFromModel(problemRecord);
    }

    @Override
    public String getStatement(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(statementDir, "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read statement!");
        }

        return statement;
    }

    @Override
    public String getStatement(String problemJid) {
        ProblemModel problemRecord = dao.findByJid(problemJid);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(statementDir, "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read statement!");
        }

        return statement;
    }

    @Override
    public GradingConfig getGradingConfig(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        String json;
        try {
            json = FileUtils.readFileToString(new File(gradingDir, "config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading!");
        }

        return GraderRegistry.getInstance().getGrader(problemRecord.gradingType).createGradingConfigFromJson(json);
    }

    @Override
    public long getGradingLastUpdateTime(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File gradingLastUpdateTimeFile = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "lastUpdateTime.txt");
        try {
            return Long.parseLong(FileUtils.readFileToString(gradingLastUpdateTimeFile));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading last update time");
        }
    }

    @Override
    public void updateStatement(long id, String statement) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        try {
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }

        updateProblemRecord(problemRecord, false);
    }

    @Override
    public void uploadTestDataFile(long id, File testDataFile, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        File testDataDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "testdata");
        try {
            FileUtils.copyFile(testDataFile, new File(testDataDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void uploadTestDataFileZipped(long id, File testDataFileZipped) {
        ProblemModel problemRecord = dao.findById(id);
        File testDataDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "testdata");

        uploadZippedFiles(testDataDir, testDataFileZipped);

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void uploadHelperFile(long id, File helperFile, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        File helperDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "helper");
        try {
            FileUtils.copyFile(helperFile, new File(helperDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void uploadHelperFileZipped(long id, File helperFileZipped) {
        ProblemModel problemRecord = dao.findById(id);
        File helperDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "helper");

        uploadZippedFiles(helperDir, helperFileZipped);

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void uploadMediaFile(long id, File mediaFile, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        File mediaDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "statement", "media");
        try {
            FileUtils.copyFile(mediaFile, new File(mediaDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void uploadMediaFileZipped(long id, File mediaFileZipped) {
        ProblemModel problemRecord = dao.findById(id);
        File mediaDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "statement", "media");

        uploadZippedFiles(mediaDir, mediaFileZipped);

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public void updateGradingConfig(long id, GradingConfig config) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }

        updateProblemRecord(problemRecord, true);
    }

    @Override
    public List<File> getTestDataFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File testDataDir = FileUtils.getFile(problemsDir, problemRecord.jid, "grading", "testdata");

        if (!testDataDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(testDataDir.listFiles());
    }

    @Override
    public List<File> getHelperFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File helpersDir = FileUtils.getFile(problemsDir, problemRecord.jid, "grading", "helper");

        if (!helpersDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(helpersDir.listFiles());
    }

    @Override
    public List<File> getMediaFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File mediaDir = FileUtils.getFile(problemsDir, problemRecord.jid, "statement", "media");

        if (!mediaDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(mediaDir.listFiles());
    }


    @Override
    public File getTestDataFile(long id, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "testdata", filename);
    }


    @Override
    public File getHelperFile(long id, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "helper", filename);
    }


    @Override
    public File getMediaFile(long id, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "statement", "media", filename);
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

    private void createProblemDirs(ProblemModel problemRecord) {
        createProblemGradingDir(problemRecord);
        createProblemStatementDir(problemRecord);
    }

    private void createProblemGradingDir(ProblemModel problemRecord) {
        File gradingDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading");

        try {
            FileUtils.forceMkdir(gradingDir);
            FileUtils.forceMkdir(new File(gradingDir, "testdata"));
            FileUtils.forceMkdir(new File(gradingDir, "helper"));

            GradingConfig config = GraderRegistry.getInstance().getGrader(problemRecord.gradingType).createDefaultGradingConfig();

            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
            FileUtils.writeStringToFile(new File(gradingDir, "lastUpdateTime.txt"), "" + problemRecord.timeCreate);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }
    }

    private void createProblemStatementDir(ProblemModel problemRecord) {
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File statementDir = FileUtils.getFile(problemsDir, problemRecord.jid, "statement");

        try {
            FileUtils.forceMkdir(statementDir);
            FileUtils.forceMkdir(new File(statementDir, "media"));
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), "Problem description here");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }
    }
    
    private Problem createProblemFromModel(ProblemModel record) {
        return new Problem(record.id, record.jid, record.name, record.gradingType, record.timeUpdate, record.additionalNote);
    }

    private List<File> getGradingFiles(String problemJid) {
        ProblemModel problemRecord = dao.findByJid(problemJid);
        File gradingDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading");

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

    private void uploadZippedFiles(File targetDir, File zippedFiles) {
        byte[] buffer = new byte[4096];
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedFiles));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String filename = ze.getName();
                File file = new File(targetDir, filename);

                // only process outer files
                if (file.isDirectory() || targetDir.getAbsolutePath().equals(file.getParentFile().getAbsolutePath())) {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProblemRecord(ProblemModel problemRecord, boolean isRelatedToGrading) {
        dao.edit(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        if (isRelatedToGrading) {
            File lastUpdateTimeFile = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemRecord.jid, "grading", "lastUpdateTime.txt");

            try {
                FileUtils.forceDelete(lastUpdateTimeFile);
                FileUtils.writeStringToFile(lastUpdateTimeFile, "" + problemRecord.timeUpdate);
            } catch (IOException e) {
                throw new RuntimeException("Cannot update lastUpdateTime.txt");
            }
        }
    }
}
