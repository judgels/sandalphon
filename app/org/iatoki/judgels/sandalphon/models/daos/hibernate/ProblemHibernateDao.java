package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.helpers.Page;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.metas.MetaProblem;
import org.iatoki.judgels.sandalphon.models.domains.Problem;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public final class ProblemHibernateDao extends AbstractHibernateDao<String, Problem>
        implements ProblemDao {

    @Override
    public List<Long> findProblemIdByTitle(String title) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Problem> query = cb.createQuery(getEntityClass());

        Root<Problem> problemRoot = query.from(getEntityClass());

        query
            .multiselect(problemRoot.get(MetaProblem.problemId.getName()))
            .where(cb.equal(problemRoot.get(MetaProblem.title), title)).from(getEntityClass());

        List<Problem> problemsList = JPA.em().createQuery(query).getResultList();

        List<Long> result = new ArrayList<>();
        for (Problem p : problemsList) {
            result.add(p.id);
        }

        return result;
    }

    @Override
    public Page<Problem> page(int page, int pageSize, String sortBy, String order, String filter) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        CriteriaQuery<Problem> query = cb.createQuery(getEntityClass());

        Root<Problem> problem = query.from(getEntityClass());

        countQuery.select(cb.count(countQuery.where(cb.like(problem.get(MetaProblem.title), "%" + filter + "%")).from(getEntityClass())));
        Long count = JPA.em().createQuery(countQuery).getSingleResult();

        Order orderBy = null;
        if ("asc".equals(order)) {
            orderBy = cb.asc(problem.get(sortBy));
        } else {
            orderBy = cb.desc(problem.get(sortBy));
        }

        query
            .where(cb.like(problem.get(MetaProblem.title), "%" + filter + "%"))
            .orderBy(orderBy)
            .from(getEntityClass());

        List<Problem> list = JPA.em().createQuery(query).setFirstResult((page - 1) * pageSize).setMaxResults(pageSize).getResultList();

        return new Page<Problem>(list, count, page, pageSize);
    }
}
