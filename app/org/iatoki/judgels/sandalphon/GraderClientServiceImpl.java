package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.GraderClientDao;
import org.iatoki.judgels.sandalphon.models.domains.GraderClientModel;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GraderClientServiceImpl implements GraderClientService {

    private GraderClientDao clientDao;

    public GraderClientServiceImpl(GraderClientDao clientDao) {
        this.clientDao = clientDao;
    }

    @Override
    public boolean isGraderClientExist(String clientJid) {
        return clientDao.isExistByClientJid(clientJid);
    }

    @Override
    public GraderClient findGraderClientById(long clientId) {
        GraderClientModel clientModel = clientDao.findById(clientId);

        return new GraderClient(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }

    @Override
    public GraderClient findGraderClientByJid(String clientJid) {
        GraderClientModel clientModel = clientDao.findByJid(clientJid);

        return new GraderClient(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }

    @Override
    public void createGraderClient(String name) {
        GraderClientModel clientModel = new GraderClientModel();
        clientModel.name = name;
        clientModel.secret = SandalphonUtils.hashMD5(UUID.randomUUID().toString());

        clientDao.persist(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateGraderClient(long clientId, String name) {
        GraderClientModel clientModel = clientDao.findById(clientId);
        clientModel.name = name;

        clientDao.edit(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<GraderClient> pageGraderClient(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = clientDao.countByFilter(filterString);
        List<GraderClientModel> clientModel = clientDao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<GraderClient> clients = clientModel
                .stream()
                .map(c -> new GraderClient(c.id, c.jid, c.name, c.secret)).collect(Collectors.toList());

        return new Page<>(clients, totalPage, page, pageSize);
    }

    @Override
    public boolean verifyGraderClient(String clientJid, String clientSecret) {
        GraderClientModel graderClientRecord = clientDao.findByJid(clientJid);

        return graderClientRecord != null && graderClientRecord.secret.equals(clientSecret);
    }
}
