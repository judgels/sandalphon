package org.iatoki.judgels.sandalphon;

final class ProblemServiceProviderImpl implements ProblemServiceProvider {

    private final ProgrammingProblemService programmingProblemService;

    public ProblemServiceProviderImpl(ProgrammingProblemService programmingProblemService) {
        this.programmingProblemService = programmingProblemService;
    }

    @Override
    public ProblemService getByType(String type) {
        if (type.equals("programming")) {
            return programmingProblemService;
        } else {
            throw new IllegalArgumentException("Unknown problem type: " + type);
        }
    }
}
