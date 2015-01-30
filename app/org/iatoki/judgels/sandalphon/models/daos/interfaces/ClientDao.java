package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.ClientModel;

import java.util.List;

public interface ClientDao extends JudgelsDao<ClientModel> {

    boolean isExistByClientJid(String clientJid);

    long countByFilter(String filterString);

    List<ClientModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
