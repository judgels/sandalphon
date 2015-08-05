package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ProblemPartnerUsernameForm {
    @Constraints.Required
    public String username;
}
