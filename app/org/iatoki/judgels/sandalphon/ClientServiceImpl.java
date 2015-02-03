package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ClientModel;
import org.iatoki.judgels.sandalphon.models.domains.ClientProblemModel;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ClientServiceImpl implements ClientService {

    private ClientDao clientDao;
    private ClientProblemDao clientProblemDao;

    public ClientServiceImpl(ClientDao clientDao, ClientProblemDao clientProblemDao) {
        this.clientDao = clientDao;
        this.clientProblemDao = clientProblemDao;
    }

    @Override
    public boolean isClientExist(String clientJid) {
        return clientDao.isExistByClientJid(clientJid);
    }

    @Override
    public List<Client> findAllClients() {
        List<ClientModel> clientModels = clientDao.findAll();

        return clientModels.stream().map(c -> new Client(c.id, c.jid, c.name, c.secret)).collect(Collectors.toList());
    }

    @Override
    public Client findClientById(long clientId) {
        ClientModel clientModel = clientDao.findById(clientId);

        return new Client(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }

    @Override
    public Client findClientByJid(String clientJid) {
        ClientModel clientModel = clientDao.findByJid(clientJid);

        return new Client(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }

    @Override
    public void createClient(String name) {
        ClientModel clientModel = new ClientModel();
        clientModel.name = name;
        clientModel.secret = SandalphonUtils.hashMD5(UUID.randomUUID().toString());

        clientDao.persist(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateClient(long clientId, String name) {
        ClientModel clientModel = clientDao.findById(clientId);
        clientModel.name = name;

        clientDao.edit(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void deleteClient(long clientId) {
        ClientModel clientModel = clientDao.findById(clientId);

        clientDao.remove(clientModel);
    }

    @Override
    public Page<Client> pageClient(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = clientDao.countByFilter(filterString);
        List<ClientModel> clientModel = clientDao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Client> clients = clientModel
                .stream()
                .map(c -> new Client(c.id, c.jid, c.name, c.secret)).collect(Collectors.toList());

        return new Page<>(clients, totalPage, page, pageSize);
    }

    @Override
    public boolean isClientProblemInProblemByClientJid(String problemJid, String clientJid) {
        return clientProblemDao.isExistByClientJid(problemJid, clientJid);
    }

    @Override
    public ClientProblem findClientProblemByClientJidAndProblemJid(String clientJid, String problemJid) {
        ClientProblemModel clientProblemModel = clientProblemDao.findByClientJidAndProblemJid(clientJid, problemJid);
        ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);

        return new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret);
    }

    @Override
    public ClientProblem findClientProblemByClientProblemId(long clientProblemId) {
        ClientProblemModel clientProblemModel = clientProblemDao.findById(clientProblemId);
        ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);

        return new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret);
    }

    @Override
    public List<ClientProblem> findAllClientProblemByProblemId(String problemJid) {
        List<ClientProblemModel> clientProblemModels = clientProblemDao.findByProblemJid(problemJid);
        ImmutableList.Builder<ClientProblem> clientProblemBuilder = ImmutableList.builder();

        for (ClientProblemModel clientProblemModel : clientProblemModels) {
            ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);
            clientProblemBuilder.add(new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret));
        }

        return clientProblemBuilder.build();
    }

    @Override
    public void createClientProblem(String problemJid, String clientJid) {
        ClientProblemModel clientProblemModel = new ClientProblemModel();
        clientProblemModel.problemJid = problemJid;
        clientProblemModel.clientJid = clientJid;
        clientProblemModel.secret = SandalphonUtils.hashMD5(UUID.randomUUID().toString());

        clientProblemDao.persist(clientProblemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateClientProblem(long clientProblemId, String clientJid) {
        ClientProblemModel clientProblemModel = clientProblemDao.findById(clientProblemId);
        clientProblemModel.clientJid = clientJid;
        clientProblemModel.secret = SandalphonUtils.hashMD5(UUID.randomUUID().toString());

        clientProblemDao.persist(clientProblemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

    }

    @Override
    public void deleteClientProblem(long clientProblemId) {
        ClientProblemModel clientProblemModel = clientProblemDao.findById(clientProblemId);
        clientProblemDao.remove(clientProblemModel);
    }
}
