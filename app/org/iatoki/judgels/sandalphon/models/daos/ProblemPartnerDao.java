package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.Dao;
import org.iatoki.judgels.sandalphon.models.entities.ProblemPartnerModel;

import java.util.List;

public interface ProblemPartnerDao extends Dao<Long, ProblemPartnerModel> {

    boolean existsByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    ProblemPartnerModel findByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    List<String> getProblemJidsByPartnerJid(String partnerJid);
}
