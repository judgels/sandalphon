package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class GraderUpsertForm {

    @Constraints.Required
    public String name;
}
