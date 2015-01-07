package org.iatoki.judgels.sandalphon.forms.programming;

import play.data.validation.Constraints;

public final class UpsertForm {

    @Constraints.Required
    public String name;

    public String gradingMethod;

    public String note;
}
