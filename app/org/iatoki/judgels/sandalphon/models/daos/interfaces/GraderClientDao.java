package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.GraderClientModel;

import java.util.List;

public interface GraderClientDao extends JudgelsDao<GraderClientModel> {

    boolean isExistByClientJid(String clientJid);

    long countByFilter(String filterString);

    List<GraderClientModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
