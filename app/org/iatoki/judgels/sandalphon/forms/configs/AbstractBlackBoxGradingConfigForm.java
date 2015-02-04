package org.iatoki.judgels.sandalphon.forms.configs;

import java.util.List;

public abstract class AbstractBlackBoxGradingConfigForm {
    public int timeLimit;

    public int memoryLimit;

    public List<String> sampleTestCaseInputs;

    public List<String> sampleTestCaseOutputs;

    public List<List<String>> testCaseInputs;

    public List<List<String>> testCaseOutputs;
}
