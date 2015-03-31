package org.iatoki.judgels.sandalphon.forms.programming;

import play.data.validation.Constraints;

import java.util.Map;

public final class ProgrammingUpsertForm {

    @Constraints.Required
    public String name;

    public String gradingEngine;

    public String additionalNote;

    public Map<String, String> allowedLanguageNames;

    public boolean isAllowedAll;
}
