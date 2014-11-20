package org.iatoki.judgels.sandalphon.models.dao.interfaces;

import org.iatoki.judgels.commons.helpers.Page;
import org.iatoki.judgels.commons.models.dao.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.schema.Problem;

import java.util.List;

public interface ProblemDao extends Dao<String, Problem> {

    List<String> findProblemIdByTitle(String title);

    Page<Problem> page(int page, int pageSize, String sortBy, String order, String filter);

}
