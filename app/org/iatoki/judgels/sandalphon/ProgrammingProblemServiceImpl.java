package org.iatoki.judgels.sandalphon;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ProgrammingProblemServiceImpl implements ProgrammingProblemService {

    private final ProblemDao problemDao;
    private final ProgrammingProblemDao programmingProblemDao;  /* currently unused now */
    private final File baseDir;

    public ProgrammingProblemServiceImpl(ProblemDao problemDao, ProgrammingProblemDao programmingProblemDao, File baseDir) {
        this.problemDao = problemDao;
        this.programmingProblemDao = programmingProblemDao;
        this.baseDir = baseDir;
    }

    @Override
    public Problem createProblem(String name, String note) {
        ProblemModel problemRecord = new ProblemModel(name, note, "PROGRAMMING");
        problemDao.persist(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        try {
            FileUtils.forceMkdir(new File(baseDir, problemRecord.jid));
            FileUtils.forceMkdir(new File(new File(baseDir, problemRecord.jid), "testcases"));
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "statement.html"), "Keren parah");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return new ProblemImpl(problemRecord);
    }

    @Override
    public String getProblemStatement(long id) {
        ProblemModel problemRecord = problemDao.findById(id);
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
        ProblemModel problemRecord = problemDao.findById(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }
    }

    @Override
    public void uploadGradingFile(long id, File file, String filename) {
        ProblemModel problemRecord = problemDao.findById(id);
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

        ProblemModel problemRecord = problemDao.findById(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, problemRecord.jid), "grading.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }
    }

    @Override
    public List<String> getGradingFilenames(long id) {
        ProblemModel problemRecord = problemDao.findById(id);

        File testcasesDir  = new File(new File(baseDir, problemRecord.jid), "testcases");

        return Lists.transform(Arrays.asList(testcasesDir.listFiles()), f -> f.getName());
    }

    private static class ProblemImpl implements Problem {

        private final ProblemModel problemModel;

        ProblemImpl(ProblemModel problemModel) {
            this.problemModel = problemModel;
        }

        @Override
        public long getId() {
            return problemModel.id;
        }

        @Override
        public String getJid() {
            return problemModel.jid;
        }

        @Override
        public String getName() {
            return problemModel.name;
        }

        @Override
        public String getNote() {
            return problemModel.note;
        }

        @Override
        public ProblemType getType() {
            if (problemModel.type.equals("PROGRAMMING")) {
                return ProblemType.PROGRAMMING;
            } else {
                return ProblemType.MULTIPLE_CHOICE;
            }
        }
    }
}
