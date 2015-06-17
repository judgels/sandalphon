package org.iatoki.judgels.sandalphon.forms;

import org.iatoki.judgels.sandalphon.ClientProblem;
import play.data.validation.Constraints;

public final class ClientLessonUpsertForm {

    public ClientLessonUpsertForm() {
    }

    public ClientLessonUpsertForm(ClientProblem clientProblem) {
        this.clientJid = clientProblem.getClientJid();
    }

    @Constraints.Required
    public String clientJid;
}
