package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.EntityNotFoundException;

public final class GraderNotFoundException extends EntityNotFoundException {

    public GraderNotFoundException() {
        super();
    }

    public GraderNotFoundException(String s) {
        super(s);
    }

    public GraderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraderNotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getEntityName() {
        return "Grader";
    }
}
