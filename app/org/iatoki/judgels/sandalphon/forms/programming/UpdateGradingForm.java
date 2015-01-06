package org.iatoki.judgels.sandalphon.forms.programming;

import play.data.validation.Constraints;

import java.util.List;

public final class UpdateGradingForm {

    @Constraints.Required
    public int timeLimit;

    @Constraints.Required
    public int memoryLimit;

    public List<List<String>> tcIn;

    public List<List<String>> tcOut;

    public List<List<Integer>> subtaskBatches;

    public List<Integer> subtaskPoints;
}
