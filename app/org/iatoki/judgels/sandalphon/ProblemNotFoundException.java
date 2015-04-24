package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.EntityNotFoundException;

public final class ProblemNotFoundException extends EntityNotFoundException {

    public ProblemNotFoundException() {
        super();
    }

    public ProblemNotFoundException(String s) {
        super(s);
    }

    public ProblemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProblemNotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getEntityName() {
        return "Problem";
    }
}
