package org.iatoki.judgels.sandalphon;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class ProgrammingProblemServiceImpl extends ProblemServiceImpl implements ProgrammingProblemService {

    private final ProgrammingProblemDao programmingProblemDao;  /* currently unused now */
    private final File baseDir;

    public ProgrammingProblemServiceImpl(ProblemDao problemDao, ProgrammingProblemDao programmingProblemDao, File baseDir) {
        super(problemDao);
        this.programmingProblemDao = programmingProblemDao;
        this.baseDir = baseDir;
    }

    @Override
    public Problem getProblem(long id) {
        ProblemModel baseProblemModel = super.getBaseProblem(id);
        return new ProgrammingProblemImpl(baseProblemModel);
    }

    @Override
    public Problem createProblem(String name, String note) {
        ProblemModel baseProblemModel = super.createBaseProblem(name, note, "programming");
        try {
            FileUtils.forceMkdir(new File(baseDir, baseProblemModel.jid));
            FileUtils.writeStringToFile(new File(new File(baseDir, baseProblemModel.jid), "statement.html"), "Keren parah");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return new ProgrammingProblemImpl(baseProblemModel);
    }

    @Override
    public ProgrammingProblemStatement getProblemStatement(long id) {
        ProblemModel baseProblemModel = super.getBaseProblem(id);
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(new File(baseDir, baseProblemModel.jid), "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read statement!");
        }

        return new ProgrammingProblemStatementImpl(baseProblemModel, statement);
    }

    @Override
    public void updateProblemStatement(long id, String statement) {
        ProblemModel baseProblemModel = super.getBaseProblem(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, baseProblemModel.jid), "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }
    }

    @Override
    public void uploadGradingFile(long id, File file, String filename) {
        ProblemModel baseProblemModel = super.getBaseProblem(id);
        try {
            FileUtils.copyFile(file, new File(new File(new File(baseDir, baseProblemModel.jid), "testdata"), filename));
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

        ProblemModel baseProblemModel = super.getBaseProblem(id);
        try {
            FileUtils.writeStringToFile(new File(new File(baseDir, baseProblemModel.jid), "grading.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }
    }

    @Override
    public List<String> getGradingFilenames(long id) {
        ProblemModel baseProblemModel = super.getBaseProblem(id);

        File testdataDir  = new File(new File(baseDir, baseProblemModel.jid), "testdata");

        return Lists.transform(Arrays.asList(testdataDir.listFiles()), f -> f.getName());
    }
}
