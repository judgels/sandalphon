package org.iatoki.judgels.sandalphon;

import java.util.List;

public final class Subtask {
    private final int points;
    private final List<Integer> batches;

    public Subtask(int points, List<Integer> batches) {
        this.points = points;
        this.batches = batches;
    }

    public int getPoints() {
        return points;
    }

    public List<Integer> getBatches() {
        return batches;
    }
}
