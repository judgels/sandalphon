package org.iatoki.judgels.sandalphon.programming.forms;

import play.data.validation.Constraints;

import java.util.Map;

public final class UpsertForm {

    @Constraints.Required
    public String name;

    public String gradingEngine;

    public String additionalNote;

    public Map<String, String> allowedLanguageNames;

    public boolean isAllowedAll;
}
