package org.iatoki.judgels.sandalphon.models.daos.programming.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemSubmissionModel;

import java.util.List;

public interface ProblemSubmissionDao extends JudgelsDao<ProblemSubmissionModel> {

    List<ProblemSubmissionModel> findByProblem(String problemJid);

    long countByFilter(String filterString);

    List<ProblemSubmissionModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
