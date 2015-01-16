package org.iatoki.judgels.sandalphon;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingExecutor;
import org.iatoki.judgels.gabriel.GradingRegistry;
import org.iatoki.judgels.gabriel.blackbox.BlackBoxGradingRequest;
import org.iatoki.judgels.gabriel.blackbox.OverallVerdict;
import org.iatoki.judgels.gabriel.grading.batch.BatchGradingConfig;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblemModel;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingSubmissionModel;
import org.iatoki.judgels.sealtiel.client.ClientMessage;

import javax.swing.text.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProgrammingProblemDao dao;
    private final ProgrammingSubmissionDao submissionDao;

    public ProgrammingProblemServiceImpl(ProgrammingProblemDao dao, ProgrammingSubmissionDao submissionDao) {
        this.dao = dao;
        this.submissionDao = submissionDao;
    }

    @Override
    public final ProgrammingProblem findProblemById(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        return createProblemFromModel(problemRecord);
    }

    @Override
    public final void updateProblem(long id, String name, String note) {
        ProgrammingProblemModel model = dao.findById(id);
        model.name = name;
        model.note = note;
        dao.edit(model, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<ProgrammingProblem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = dao.countByFilter(filterString);
        List<ProgrammingProblemModel> problemRecords = dao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<ProgrammingProblem> problems = problemRecords
                .stream()
                .map(problemRecord -> createProblemFromModel(problemRecord))
                .collect(Collectors.toList());

        return new Page<>(problems, totalPage, page, pageSize);
    }


    @Override
    public ProgrammingProblem createProblem(String name, String gradingMethod, String note) {
        ProgrammingProblemModel problemRecord = new ProgrammingProblemModel(name, gradingMethod, note);
        dao.persist(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        File gradingDir = new File(problemDir, "grading");

        try {
            FileUtils.forceMkdir(problemDir);
            FileUtils.forceMkdir(gradingDir);
            FileUtils.forceMkdir(new File(problemDir, "statement"));
            FileUtils.forceMkdir(new File(gradingDir, "testData"));
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), "Keren parah");

            Gson gson = new Gson();
            String json = gson.toJson(new BatchGradingConfig());

            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return createProblemFromModel(problemRecord);
    }

    @Override
    public String getProblemStatement(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
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
    public String getProblemGrading(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        String json;
        try {
            json = FileUtils.readFileToString(new File(gradingDir, "config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading!");
        }

        return json;
    }

    @Override
    public void updateProblemStatement(long id, String statement) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        try {
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }
    }

    @Override
    public void uploadGradingFile(long id, File file, String filename) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.copyFile(file, new File(new File(gradingDir, "testData"), filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateProblemGrading(long id, String json) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }
    }

    @Override
    public List<String> getGradingFilenames(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        File testDataDir = new File(gradingDir, "testData");

        return Lists.transform(Arrays.asList(testDataDir.listFiles()), f -> f.getName());
    }

    @Override
    public void submit(String problemJid, Map<String, byte[]> sourceFiles) {
        FakeSealtiel sealtiel = new FakeSealtiel();

        ProgrammingSubmissionModel record = new ProgrammingSubmissionModel();
        record.problemJid = problemJid;
        record.score = 0;
        record.verdict = OverallVerdict.PENDING;

        submissionDao.persist(record, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        BlackBoxGradingRequest request = new BlackBoxGradingRequest("sfsdf", record.jid, problemJid, "Batch", "Cpp", sourceFiles);

        ClientMessage message = new ClientMessage("sdfdsf", "BlackBoxGradingRequest", new Gson().toJson(request));
        sealtiel.sendMessage(message);
    }

    private ProgrammingProblem createProblemFromModel(ProgrammingProblemModel record) {
        GradingExecutor gradingMethod = GradingRegistry.getInstance().getGradingExecutor(record.gradingMethod);
        return new ProgrammingProblem(record.id, record.jid, record.name, gradingMethod, record.note);
    }
}
