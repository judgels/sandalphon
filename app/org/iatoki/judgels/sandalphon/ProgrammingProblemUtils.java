package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.iatoki.judgels.gabriel.blackbox.Subtask;
import org.iatoki.judgels.gabriel.blackbox.TestCase;
import org.iatoki.judgels.gabriel.blackbox.TestSet;
import org.iatoki.judgels.gabriel.grading.batch.BatchGradingConfig;
import org.iatoki.judgels.sandalphon.forms.grading.BatchGradingConfigForm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProgrammingProblemUtils {

    public static BatchGradingConfig toGradingConfig(BatchGradingConfigForm form) {
        int testSetsCount = form.testCaseInputs.size();
        ImmutableList.Builder<TestSet> testSets = ImmutableList.builder();

        for (int i = 0; i < testSetsCount; i++) {
            Set<Integer> subtasks = form.testSetSubtasks.get(i).stream()
                    .filter(s -> s != null)
                    .collect(Collectors.toSet());

            ImmutableList.Builder<TestCase> testCases = ImmutableList.builder();
            for (int j = 0; j < form.testCaseInputs.get(i).size(); j++) {
                testCases.add(new TestCase(form.testCaseInputs.get(i).get(j), form.testCaseOutputs.get(i).get(j)));
            }

            testSets.add(new TestSet(testCases.build(), subtasks));
        }

        ImmutableList.Builder<Subtask> subtasks = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            subtasks.add(new Subtask(form.subtaskPoints.get(i), form.subtaskParams.get(i)));
        }

        return new BatchGradingConfig(form.timeLimit, form.memoryLimit, testSets.build(), subtasks.build());
    }

    public static BatchGradingConfigForm toGradingForm(BatchGradingConfig config) {
        BatchGradingConfigForm form = new BatchGradingConfigForm();

        form.timeLimit = config.getTimeLimitInMilliseconds();
        form.memoryLimit = config.getMemoryLimitInKilobytes();

        ImmutableList.Builder<List<String>> testCasesIn = ImmutableList.builder();
        ImmutableList.Builder<List<String>> testCasesOut = ImmutableList.builder();
        ImmutableList.Builder<List<Integer>> testSetsSubtasks = ImmutableList.builder();

        for (TestSet testSet : config.getTestData()) {
            testCasesIn.add(testSet.getTestCases().stream().map(testCase -> testCase.getInput()).collect(Collectors.toList()));
            testCasesOut.add(testSet.getTestCases().stream().map(testCase -> testCase.getOutput()).collect(Collectors.toList()));

            List<Integer> subtasks = Lists.newArrayList();
            for (int j = 0; j < 10; j++) {
                if (testSet.getSubtasks().contains(j)) {
                    subtasks.add(j);
                } else {
                    subtasks.add(null);
                }
            }
            testSetsSubtasks.add(subtasks);
        }

        ImmutableList.Builder<Double> subtaskPoints = ImmutableList.builder();
        ImmutableList.Builder<String> subtaskParams = ImmutableList.builder();

        for (Subtask subtask : config.getSubtasks()) {
            subtaskPoints.add(subtask.getPoints());
            subtaskParams.add(subtask.getParam());
        }

        form.testCaseInputs = testCasesIn.build();
        form.testCaseOutputs = testCasesOut.build();
        form.testSetSubtasks = testSetsSubtasks.build();
        form.subtaskPoints = subtaskPoints.build();
        form.subtaskParams = subtaskParams.build();

        return form;
    }
}
