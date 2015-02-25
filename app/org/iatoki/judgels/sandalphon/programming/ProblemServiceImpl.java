package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.NaturalFilenameComparator;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;

    public ProblemServiceImpl(ProblemDao dao) {
        this.dao = dao;
    }

    @Override
    public boolean problemExistsByJid(String problemJid) {
        return dao.existsByJid(problemJid);
    }

    @Override
    public final Problem findProblemById(long problemId) {
        ProblemModel problemModel = dao.findById(problemId);
        return createProblemFromModel(problemModel);
    }

    @Override
    public final Problem findProblemByJid(String problemJid) {
        ProblemModel problemModel = dao.findByJid(problemJid);
        return createProblemFromModel(problemModel);
    }

    @Override
    public final void updateProblem(long id, String name, String gradingEngine, String additionalNote) {
        ProblemModel model = dao.findById(id);
        model.name = name;

        if (!gradingEngine.equals(model.gradingEngine)) {
            updateGradingConfig(id, GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig());
        }

        model.gradingEngine = gradingEngine;
        model.additionalNote = additionalNote;
        updateProblemModel(model, false);
    }

    @Override
    public Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalPages = dao.countByFilters(filterString, ImmutableMap.of());
        List<ProblemModel> problemModels = dao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<Problem> problems = Lists.transform(problemModels, m -> createProblemFromModel(m));

        return new Page<>(problems, totalPages, pageIndex, pageSize);
    }

    @Override
    public Problem createProblem(String name, String gradingEngine, String additionalNote) {
        ProblemModel problemModel = new ProblemModel(name, gradingEngine, additionalNote);
        dao.persist(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        createProblemDirs(problemModel);

        return createProblemFromModel(problemModel);
    }

    @Override
    public String getStatement(long problemId) {
        ProblemModel problemModel = dao.findById(problemId);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemModel.jid);
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
        ProblemModel problemModel = dao.findByJid(problemJid);
        return getStatement(problemModel.id);
    }

    @Override
    public GradingConfig getGradingConfig(long id) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemModel.jid);
        File gradingDir = new File(problemDir, "grading");
        String json;
        try {
            json = FileUtils.readFileToString(new File(gradingDir, "config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading!");
        }

        return GradingEngineRegistry.getInstance().getEngine(problemModel.gradingEngine).createGradingConfigFromJson(json);
    }

    @Override
    public Date getGradingLastUpdateTime(String problemJid) {
        ProblemModel problemModel = dao.findByJid(problemJid);
        File gradingLastUpdateTimeFile = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "lastUpdateTime.txt");
        try {
            return new Date(Long.parseLong(FileUtils.readFileToString(gradingLastUpdateTimeFile)));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading last update time");
        }
    }

    @Override
    public void updateStatement(long id, String statement) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemModel.jid);
        File statementDir = new File(problemDir, "statement");
        try {
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }

        updateProblemModel(problemModel, false);
    }

    @Override
    public void uploadTestDataFile(long id, File testDataFile, String filename) {
        ProblemModel problemModel = dao.findById(id);
        File testDataDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "testdata");
        try {
            FileUtils.copyFile(testDataFile, new File(testDataDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel, true);
    }

    @Override
    public void uploadTestDataFileZipped(long id, File testDataFileZipped) {
        ProblemModel problemModel = dao.findById(id);
        File testDataDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "testdata");

        uploadZippedFiles(testDataDir, testDataFileZipped);

        updateProblemModel(problemModel, true);
    }

    @Override
    public void uploadHelperFile(long id, File helperFile, String filename) {
        ProblemModel problemModel = dao.findById(id);
        File helperDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "helper");
        try {
            FileUtils.copyFile(helperFile, new File(helperDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel, true);
    }

    @Override
    public void uploadHelperFileZipped(long id, File helperFileZipped) {
        ProblemModel problemModel = dao.findById(id);
        File helperDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "helper");

        uploadZippedFiles(helperDir, helperFileZipped);

        updateProblemModel(problemModel, true);
    }

    @Override
    public void uploadMediaFile(long id, File mediaFile, String filename) {
        ProblemModel problemModel = dao.findById(id);
        File mediaDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "statement", "media");
        try {
            FileUtils.copyFile(mediaFile, new File(mediaDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateProblemModel(problemModel, true);
    }

    @Override
    public void uploadMediaFileZipped(long id, File mediaFileZipped) {
        ProblemModel problemModel = dao.findById(id);
        File mediaDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "statement", "media");

        uploadZippedFiles(mediaDir, mediaFileZipped);

        updateProblemModel(problemModel, true);
    }

    @Override
    public void updateGradingConfig(long id, GradingConfig config) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File problemDir = new File(problemsDir, problemModel.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }

        updateProblemModel(problemModel, true);
    }

    @Override
    public List<File> getTestDataFiles(long id) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File testDataDir = FileUtils.getFile(problemsDir, problemModel.jid, "grading", "testdata");
        Comparator<String> comparator = new NaturalFilenameComparator();

        List<File> files = Arrays.asList(testDataDir.listFiles());
        Collections.sort(files, (File a, File b) -> comparator.compare(a.getName(), b.getName()));

        return ImmutableList.copyOf(files);
    }

    @Override
    public List<File> getHelperFiles(long id) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File helpersDir = FileUtils.getFile(problemsDir, problemModel.jid, "grading", "helper");

        if (!helpersDir.isDirectory()) {
            return ImmutableList.of();
        }

        Comparator<String> comparator = new NaturalFilenameComparator();

        List<File> files = Arrays.asList(helpersDir.listFiles());
        Collections.sort(files, (File a, File b) -> comparator.compare(a.getName(), b.getName()));

        return ImmutableList.copyOf(files);
    }

    @Override
    public List<File> getMediaFiles(long id) {
        ProblemModel problemModel = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File mediaDir = FileUtils.getFile(problemsDir, problemModel.jid, "statement", "media");

        if (!mediaDir.isDirectory()) {
            return ImmutableList.of();
        }

        Comparator<String> comparator = new NaturalFilenameComparator();

        List<File> files = Arrays.asList(mediaDir.listFiles());
        Collections.sort(files, (File a, File b) -> comparator.compare(a.getName(), b.getName()));

        return ImmutableList.copyOf(files);
    }


    @Override
    public File getTestDataFile(long id, String filename) {
        ProblemModel problemModel = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "testdata", filename);
    }


    @Override
    public File getHelperFile(long id, String filename) {
        ProblemModel problemModel = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "helper", filename);
    }


    @Override
    public File getMediaFile(long id, String filename) {
        ProblemModel problemModel = dao.findById(id);
        return FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "statement", "media", filename);
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

    private void createProblemDirs(ProblemModel problemModel) {
        createProblemGradingDir(problemModel);
        createProblemStatementDir(problemModel);
    }

    private void createProblemGradingDir(ProblemModel problemModel) {
        File gradingDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading");

        try {
            FileUtils.forceMkdir(gradingDir);
            FileUtils.forceMkdir(new File(gradingDir, "testdata"));
            FileUtils.forceMkdir(new File(gradingDir, "helper"));

            GradingConfig config = GradingEngineRegistry.getInstance().getEngine(problemModel.gradingEngine).createDefaultGradingConfig();

            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), new Gson().toJson(config));
            FileUtils.writeStringToFile(new File(gradingDir, "lastUpdateTime.txt"), "" + problemModel.timeCreate);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }
    }

    private void createProblemStatementDir(ProblemModel problemModel) {
        File problemsDir = SandalphonProperties.getInstance().getProblemDir();
        File statementDir = FileUtils.getFile(problemsDir, problemModel.jid, "statement");

        try {
            FileUtils.forceMkdir(statementDir);
            FileUtils.forceMkdir(new File(statementDir, "media"));
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"),

                    "<h3>Deskripsi</h3>\n" +
                            "\n" +
                            "<p>Blabla.</p>\n" +
                            "\n" +
                            "<h3>Format Masukan</h3>\n" +
                            "\n" +
                            "<p>Blabla.</p>\n" +
                            "\n" +
                            "<h3>Format Keluaran</h3>\n" +
                            "\n" +
                            "<p>Blabla.</p>\n" +
                            "\n" +
                            "<h3>Contoh Masukan</h3>\n" +
                            "\n" +
                            "<pre>\n" +
                            "Blabla.</pre>\n" +
                            "\n" +
                            "<h3>Contoh Keluaran</h3>\n" +
                            "\n" +
                            "<pre>\n" +
                            "Blabla.</pre>\n" +
                            "\n" +
                            "<h3>Batasan/Subsoal</h3>\n" +
                            "\n" +
                            "<ul>\n" +
                            "\t<li>Blabla</li>\n" +
                            "</ul>\n"
            );
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }
    }
    
    private Problem createProblemFromModel(ProblemModel record) {
        return new Problem(record.id, record.jid, record.name, record.userCreate, record.gradingEngine, new Date(record.timeUpdate), record.additionalNote);
    }

    private List<File> getGradingFiles(String problemJid) {
        ProblemModel problemModel = dao.findByJid(problemJid);
        File gradingDir = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading");

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

    private void updateProblemModel(ProblemModel problemModel, boolean isRelatedToGrading) {
        dao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        if (isRelatedToGrading) {
            File lastUpdateTimeFile = FileUtils.getFile(SandalphonProperties.getInstance().getProblemDir(), problemModel.jid, "grading", "lastUpdateTime.txt");

            try {
                FileUtils.forceDelete(lastUpdateTimeFile);
                FileUtils.writeStringToFile(lastUpdateTimeFile, "" + problemModel.timeUpdate);
            } catch (IOException e) {
                throw new RuntimeException("Cannot update lastUpdateTime.txt");
            }
        }
    }
}
