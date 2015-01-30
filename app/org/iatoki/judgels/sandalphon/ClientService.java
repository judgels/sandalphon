package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

import java.util.List;

public interface ClientService {

    boolean isClientExist(String clientJid);

    List<Client> findAllClients();

    Client findClientById(long clientId);

    Client findClientByJid(String clientJid);

    void createClient(String name);

    void updateClient(long clientId, String name);

    void deleteClient(long clientId);

    Page<Client> pageClient(long page, long pageSize, String sortBy, String order, String filterString);

    boolean isClientProblemInProblemByClientJid(String problemJid, String clientJid);

    ClientProblem findClientProblemByClientProblemId(long clientProblemId);

    List<ClientProblem> findAllClientProblemByProblemId(String problemJid);

    void createClientProblem(String problemJid, String clientJid);

    void updateClientProblem(long clientProblemId, String clientJid);

    void deleteClientProblem(long clientProblemId);

}
