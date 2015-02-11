package org.iatoki.judgels.sandalphon.programming.forms;

import play.data.validation.Constraints;

public final class UpsertForm {

    @Constraints.Required
    public String name;

    public String gradingEngine;

    public String additionalNote;
}
