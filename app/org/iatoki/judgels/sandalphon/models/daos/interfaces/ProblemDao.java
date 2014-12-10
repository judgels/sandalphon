package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.helpers.Page;
import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.Problem;

import java.util.List;

public interface ProblemDao extends JudgelsDao<Problem> {

    List<Long> findProblemIdByTitle(String title);

    Page<Problem> page(int page, int pageSize, String sortBy, String order, String filter);

}
