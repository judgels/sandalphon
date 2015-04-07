package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.util.List;

public interface ProblemDao extends JudgelsDao<ProblemModel> {
    List<String> findProblemJidsByAuthorJid(String authorJid);
}
