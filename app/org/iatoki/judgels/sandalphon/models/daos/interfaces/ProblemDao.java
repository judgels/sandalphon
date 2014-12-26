package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.util.List;

public interface ProblemDao extends JudgelsDao<ProblemModel> {

    long countByFilter(String filterString);

    List<ProblemModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
