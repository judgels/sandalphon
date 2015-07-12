package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.Dao;
import org.iatoki.judgels.sandalphon.models.entities.ClientProblemModel;

import java.util.List;

public interface ClientProblemDao extends Dao<Long, ClientProblemModel> {

    boolean existsByClientJidAndProblemJid(String clientJid, String problemJid);

    ClientProblemModel findByClientJidAndProblemJid(String clientJid, String problemJid);

    List<ClientProblemModel> findByProblemJid(String problemJid);
}
