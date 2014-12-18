package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class CreateProblemForm {

    @Constraints.Required
    public String name;

    public String note;
}
