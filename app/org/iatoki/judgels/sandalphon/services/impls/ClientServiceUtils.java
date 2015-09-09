package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.models.entities.ClientModel;

final class ClientServiceUtils {

    private ClientServiceUtils() {
        // prevent instantiation
    }

    static Client createClientFromModel(ClientModel clientModel) {
        return new Client(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }
}
