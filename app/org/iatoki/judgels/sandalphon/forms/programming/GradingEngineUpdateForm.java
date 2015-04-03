package org.iatoki.judgels.sandalphon.forms.programming;

import play.data.validation.Constraints;

public class GradingEngineUpdateForm {
    @Constraints.Required
    public String gradingEngineName;
}
