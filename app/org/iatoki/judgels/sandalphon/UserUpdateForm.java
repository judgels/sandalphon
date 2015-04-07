package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class UserUpdateForm {
    @Constraints.Required
    public String roles;
}
