package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel;

import java.util.List;

public interface ProblemDao extends JudgelsDao<ProblemModel> {
    List<String> findProblemJidsByAuthorJid(String authorJid);
}
