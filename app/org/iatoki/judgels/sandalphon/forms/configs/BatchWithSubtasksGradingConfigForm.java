package org.iatoki.judgels.sandalphon.forms.configs;

import java.util.List;

public final class BatchWithSubtasksGradingConfigForm {
    public int timeLimit;

    public int memoryLimit;

    public List<String> sampleTestCaseInputs;

    public List<String> sampleTestCaseOutputs;

    public List<List<Integer>> sampleTestCaseSubtaskIds;

    public List<List<String>> testCaseInputs;

    public List<List<String>> testCaseOutputs;

    public List<List<Integer>> testGroupSubtaskIds;

    public List<Integer> subtaskPoints;

    public String customScorer;
}
