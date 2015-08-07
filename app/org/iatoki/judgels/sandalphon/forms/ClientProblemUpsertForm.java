package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ClientProblemUpsertForm {

    @Constraints.Required
    public String clientJid;
}
