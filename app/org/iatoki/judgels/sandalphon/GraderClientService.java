package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

public interface GraderClientService {

    boolean isGraderClientExist(String clientJid);

    GraderClient findGraderClientById(long clientId);

    GraderClient findGraderClientByJid(String clientJid);

    Page<GraderClient> pageGraderClient(long page, long pageSize, String sortBy, String order, String filterString);

    void createGraderClient(String name);

    void updateGraderClient(long clientId, String name);

    boolean verifyGraderClient(String clientJid, String clientSecret);
}
