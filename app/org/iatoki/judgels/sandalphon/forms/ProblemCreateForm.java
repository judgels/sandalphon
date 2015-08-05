package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public class ProblemCreateForm {
    @Constraints.Required
    public String type;

    @Constraints.Required
    public String name;

    public String additionalNote;

    @Constraints.Required
    public String initLanguageCode;
}
