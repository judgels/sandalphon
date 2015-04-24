package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

import java.util.List;

public interface ClientService {

    boolean clientExistsByClientJid(String clientJid);

    List<Client> findAllClients();

    Client findClientById(long clientId) throws ClientNotFoundException;

    Client findClientByJid(String clientJid);

    void createClient(String name);

    void updateClient(long clientId, String name);

    void deleteClient(long clientId);

    Page<Client> pageClients(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    boolean isClientProblemInProblemByClientJid(String problemJid, String clientJid);

    ClientProblem findClientProblemByClientJidAndProblemJid(String clientJid, String problemJid);

    ClientProblem findClientProblemByClientProblemId(long clientProblemId);

    List<ClientProblem> findAllClientProblemByProblemId(String problemJid);

    void createClientProblem(String problemJid, String clientJid);

    void deleteClientProblem(long clientProblemId);
}
