package org.iatoki.judgels.sandalphon;

import play.data.validation.Constraints;

public final class ClientUpsertForm {

    public ClientUpsertForm() {

    }

    public ClientUpsertForm(Client client) {
        this.name = client.getName();
    }

    @Constraints.Required
    public String name;

}
