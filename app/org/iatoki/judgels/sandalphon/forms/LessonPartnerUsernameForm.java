package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class LessonPartnerUsernameForm {
    @Constraints.Required
    public String username;
}
