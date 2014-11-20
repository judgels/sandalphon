package org.iatoki.judgels.sandalphon.helpers.forms;

import org.iatoki.judgels.commons.helpers.forms.AbstractForm;
import play.data.validation.Constraints;

import java.util.List;

public final class ProblemForm extends AbstractForm {

    @Constraints.Required
    @Constraints.MaxLength(200)
    public String title;

    @Constraints.Required
    public String author;

    public String type;

    public String notes;

    public List<String> availableLanguages;

}
