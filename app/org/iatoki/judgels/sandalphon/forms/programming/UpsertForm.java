package org.iatoki.judgels.sandalphon.forms.programming;

import org.iatoki.judgels.gabriel.GradingType;
import play.data.validation.Constraints;

public final class UpsertForm {

    @Constraints.Required
    public String name;

    public String gradingType;

    public String additionalNote;
}
