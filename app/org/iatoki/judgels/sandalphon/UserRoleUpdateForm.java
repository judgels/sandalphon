package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class UserRoleUpdateForm {
    @Constraints.Required
    public String roles;
}
