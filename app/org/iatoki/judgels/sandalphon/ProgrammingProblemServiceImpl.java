package org.iatoki.judgels.sandalphon;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblemModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProgrammingProblemDao dao;  /* currently unused now */
    private final File baseDir;

    public ProgrammingProblemServiceImpl(ProgrammingProblemDao dao, File baseDir) {
        this.dao = dao;
        this.baseDir = baseDir;
    }

    @Override
    public final ProgrammingProblem findProblemById(long id) {
        ProgrammingProblemModel problemRecord = dao.findById(id);
        return new ProgrammingProblem(problemRecord.id, problemRecord.jid, problemRecord.name, problemRecord.note);
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
                .map(problemRecord -> new ProgrammingProblem(problemRecord.id, problemRecord.jid, problemRecord.name, problemRecord.note))
                .collect(Collectors.toList());

        return new Page<>(problems, totalPage, page, pageSize);
    }


    @Override
    public ProgrammingProblem createProblem(String name, String note) {
        ProgrammingProblemModel problemRecord = new ProgrammingProblemModel(name, note);
        dao.persist(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        try {
            FileUtils.forceMkdir(new File(baseDir, problemRecord.jid));
            FileUtils.forceMkdir(new File(new File(baseDir, problemRecord.jid), "testcases"));
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "statement.html"), "Keren parah");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return new ProgrammingProblem(problemRecord.id, problemRecord.jid, problemRecord.name, problemRecord.note);
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
    public void updateProblemGrading(long id, int timeLimit, int memoryLimit, List<List<String>> tcIn, List<List<String>> tcOut, List<List<Integer>> subtaskBatches, List<Integer> subtaskPoints) {

        List<List<TestCase>> cases = Lists.newArrayList();

        for (int i = 0; i < tcIn.size(); i++) {
            List<String> ins = tcIn.get(i);
            List<String> outs = tcOut.get(i);

            List<TestCase> tcs = Lists.newArrayList();

            for (int j = 0; j < ins.size(); j++) {
                tcs.add(new TestCase(ins.get(j), outs.get(j)));
            }

            cases.add(tcs);
        }

        List<Subtask> subtasks = Lists.newArrayList();

        for (int i = 0; i < subtaskBatches.size(); i++) {
            List<Integer> batches = subtaskBatches.get(i).stream().filter(x -> x != null).collect(Collectors.toList());
            subtasks.add(new Subtask(subtaskPoints.get(i), batches));
        }


        ProblemGrading data = new ProblemGrading(timeLimit, memoryLimit, cases, subtasks);
        String json = new Gson().toJson(data);

        ProgrammingProblemModel problemRecord = dao.findById(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "grading.json"), json);
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
}
