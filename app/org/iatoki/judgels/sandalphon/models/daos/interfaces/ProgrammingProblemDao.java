package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblemModel;

import java.util.List;

public interface ProgrammingProblemDao extends JudgelsDao<ProgrammingProblemModel> {

    long countByFilter(String filterString);

    List<ProgrammingProblemModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
