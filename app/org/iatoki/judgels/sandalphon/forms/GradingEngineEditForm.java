package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public class GradingEngineEditForm {

    @Constraints.Required
    public String gradingEngineName;
}
