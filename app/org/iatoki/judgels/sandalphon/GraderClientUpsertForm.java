package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class GraderClientUpsertForm {

    @Constraints.Required
    public String name;
}
