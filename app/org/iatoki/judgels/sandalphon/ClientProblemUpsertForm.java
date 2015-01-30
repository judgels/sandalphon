package org.iatoki.judgels.sandalphon;

public final class ClientProblemUpsertForm {

    public ClientProblemUpsertForm() {
    }

    public ClientProblemUpsertForm(ClientProblem clientProblem) {
        this.clientJid = clientProblem.getClientJid();
    }

    public String clientJid;
}
