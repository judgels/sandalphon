package org.iatoki.judgels.sandalphon.models.daos.programming.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemSubmissionModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public final class ProblemSubmissionHibernateDao extends AbstractJudgelsHibernateDao<ProblemSubmissionModel> implements ProblemSubmissionDao {

    @Override
    public List<ProblemSubmissionModel> findByProblem(String problemJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProblemSubmissionModel> query = cb.createQuery(ProblemSubmissionModel.class);

        Root<ProblemSubmissionModel> root = query.from(ProblemSubmissionModel.class);

        query = query.where(cb.equal(root.get("problemJid"), problemJid));

        return JPA.em().createQuery(query).getResultList();
    }

    @Override
    public long countByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ProblemSubmissionModel> root = query.from(ProblemSubmissionModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("problemJid"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(cb.count(root))
                .where(condition);

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<ProblemSubmissionModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProblemSubmissionModel> query = cb.createQuery(ProblemSubmissionModel.class);
        Root<ProblemSubmissionModel> root = query.from(ProblemSubmissionModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("problemJid"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        Order orderBy = null;
        if ("asc".equals(order)) {
            orderBy = cb.asc(root.get(sortBy));
        } else {
            orderBy = cb.desc(root.get(sortBy));
        }

        query
                .where(condition)
                .orderBy(orderBy);

        return JPA.em().createQuery(query).setFirstResult((int) first).setMaxResults((int) max).getResultList();
    }
}
