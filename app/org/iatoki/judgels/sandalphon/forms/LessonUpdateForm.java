package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class LessonUpdateForm {

    @Constraints.Required
    @Constraints.Pattern("^[a-z0-9]+(-[a-z0-9]+)*$")
    public String slug;

    public String additionalNote;
}
