package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class UserViewpointForm {

    @Constraints.Required
    public String username;

}
