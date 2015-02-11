package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class ClientUpsertForm {

    @Constraints.Required
    public String name;

}
