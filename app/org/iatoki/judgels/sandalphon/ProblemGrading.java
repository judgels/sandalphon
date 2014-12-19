package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class ProblemGrading {

    private final int timeLimit;

    private final int memoryLimit;

    private final List<List<TestCase>> testCases;

    private final List<Subtask> subtasks;

    public ProblemGrading(int timeLimit, int memoryLimit, List<List<TestCase>> testCases, List<Subtask> subtasks) {
        this.timeLimit = timeLimit;
        this.memoryLimit = memoryLimit;
        this.testCases = testCases;
        this.subtasks = subtasks;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }

    public List<List<TestCase>> getTestCases() {
        return ImmutableList.copyOf(testCases);
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }
}