package org.iatoki.judgels.sandalphon.models.daos.programming.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.SubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.SubmissionModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.List;

public final class SubmissionHibernateDao extends AbstractJudgelsHibernateDao<SubmissionModel> implements SubmissionDao {

    @Override
    public List<SubmissionModel> findByProblem(String problemJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<SubmissionModel> query = cb.createQuery(SubmissionModel.class);

        Root<SubmissionModel> root = query.from(SubmissionModel.class);

        query = query.where(cb.equal(root.get("problemJid"), problemJid));

        return JPA.em().createQuery(query).getResultList();
    }

    @Override
    public long countByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<SubmissionModel> root = query.from(SubmissionModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("problemJid"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(cb.count(root))
                .where(condition);

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<SubmissionModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<SubmissionModel> query = cb.createQuery(SubmissionModel.class);
        Root<SubmissionModel> root = query.from(SubmissionModel.class);

        List<Selection<?>> selection = new ArrayList<>();
        selection.add(root.get("id"));
        selection.add(root.get("jid"));
        selection.add(root.get("problemJid"));
        selection.add(root.get("verdictCode"));
        selection.add(root.get("verdictName"));
        selection.add(root.get("score"));
        selection.add(root.get("details"));

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
                .multiselect(selection)
                .where(condition)
                .orderBy(orderBy);

        return JPA.em().createQuery(query).setFirstResult((int) first).setMaxResults((int) max).getResultList();
    }
}
