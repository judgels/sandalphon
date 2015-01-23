package org.iatoki.judgels.sandalphon.models.daos.programming.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.SubmissionModel;

import java.util.List;

public interface SubmissionDao extends JudgelsDao<SubmissionModel> {

    List<SubmissionModel> findByProblem(String problemJid);

    long countByFilter(String filterString);

    List<SubmissionModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max);
}
