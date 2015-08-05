package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ProblemUpdateForm {

    @Constraints.Required
    public String name;

    public String additionalNote;
}
