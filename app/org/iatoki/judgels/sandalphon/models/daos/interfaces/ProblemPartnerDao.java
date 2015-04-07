package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel;

import java.util.List;

public interface ProblemPartnerDao extends Dao<Long, ProblemPartnerModel> {

    boolean existsByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    ProblemPartnerModel findByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    List<String> findProblemJidsByPartnerJid(String partnerJid);
}
