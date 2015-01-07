package org.iatoki.judgels.sandalphon;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingMethod;
import org.iatoki.judgels.gabriel.GradingMethodRegistry;
import org.iatoki.judgels.gabriel.grading.batch.SubtaskBatchGradingConf;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblemModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProgrammingProblemDao dao;
    private final File baseDir;

    public ProgrammingProblemServiceImpl(ProgrammingProblemDao dao, File baseDir) {
        this.dao = dao;
        this.baseDir = baseDir;
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

        try {
            FileUtils.forceMkdir(new File(baseDir, problemRecord.jid));
            FileUtils.forceMkdir(new File(new File(baseDir, problemRecord.jid), "testcases"));
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "statement.html"), "Keren parah");

            Gson gson = new Gson();
            String json = gson.toJson(new SubtaskBatchGradingConf());

            System.out.println("ASDS " + json);

            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return createProblemFromModel(problemRecord);
    }

    @Override
    public String getProblemStatement(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(new File(baseDir, problemRecord.jid), "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read statement!");
        }

        return statement;
    }

    @Override
    public String getProblemGrading(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        String json;
        try {
            json = FileUtils.readFileToString(new File(new File(baseDir, problemRecord.jid), "config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading!");
        }

        return json;
    }

    @Override
    public void updateProblemStatement(long id, String statement) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }
    }

    @Override
    public void uploadGradingFile(long id, File file, String filename) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        try {
            FileUtils.copyFile(file, new File(new File(new File(baseDir, problemRecord.jid), "testcases"), filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateProblemGrading(long id, String json) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }
    }

    @Override
    public List<String> getGradingFilenames(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);

        File testcasesDir  = new File(new File(baseDir, problemRecord.jid), "testcases");

        return Lists.transform(Arrays.asList(testcasesDir.listFiles()), f -> f.getName());
    }

    private ProgrammingProblem createProblemFromModel(ProgrammingProblemModel record) {
        GradingMethod gradingMethod = GradingMethodRegistry.getInstance().getGradingMethodByClassName(record.gradingMethod);
        return new ProgrammingProblem(record.id, record.jid, record.name, gradingMethod, record.note);
    }
}
