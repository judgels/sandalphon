package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.iatoki.judgels.gabriel.grading.batch.BatchTestCase;
import org.iatoki.judgels.gabriel.grading.batch.BatchTestSet;
import org.iatoki.judgels.gabriel.grading.batch.SubtaskBatchGradingConf;
import org.iatoki.judgels.sandalphon.forms.grading.SubtaskBatchGradingForm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProgrammingProblemUtils {

    public static SubtaskBatchGradingConf toGradingConf(SubtaskBatchGradingForm form) {
        int testSetsCount = form.testCasesIn.size();
        ImmutableList.Builder<BatchTestSet> testSets = ImmutableList.builder();

        for (int i = 0; i < testSetsCount; i++) {
            Set<Integer> subtasks = form.testSetsSubtasks.get(i).stream()
                    .filter(s -> s != null)
                    .collect(Collectors.toSet());

            ImmutableList.Builder<BatchTestCase> testCases = ImmutableList.builder();
            for (int j = 0; j < form.testCasesIn.get(i).size(); j++) {
                testCases.add(new BatchTestCase(form.testCasesIn.get(i).get(j), form.testCasesOut.get(i).get(j)));
            }

            testSets.add(new BatchTestSet(testCases.build(), subtasks));
        }

        return new SubtaskBatchGradingConf(form.timeLimit, form.memoryLimit, testSets.build());
    }

    public static SubtaskBatchGradingForm toGradingForm(SubtaskBatchGradingConf conf) {
        SubtaskBatchGradingForm form = new SubtaskBatchGradingForm();

        form.timeLimit = conf.getTimeLimit();
        form.memoryLimit = conf.getMemoryLimit();

        ImmutableList.Builder<List<String>> testCasesIn = ImmutableList.builder();
        ImmutableList.Builder<List<String>> testCasesOut = ImmutableList.builder();
        ImmutableList.Builder<List<Integer>> testSetsSubtasks = ImmutableList.builder();

        for (BatchTestSet testSet : conf.getTestSets()) {
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

        form.testCasesIn = testCasesIn.build();
        form.testCasesOut = testCasesOut.build();
        form.testSetsSubtasks = testSetsSubtasks.build();

        return form;
    }
}
