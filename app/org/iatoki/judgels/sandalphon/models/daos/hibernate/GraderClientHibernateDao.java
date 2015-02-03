package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel_;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.GraderClientDao;
import org.iatoki.judgels.sandalphon.models.domains.GraderClientModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public final class GraderClientHibernateDao extends AbstractJudgelsHibernateDao<GraderClientModel> implements GraderClientDao {

    @Override
    public boolean isExistByClientJid(String clientJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<GraderClientModel> root = query.from(GraderClientModel.class);

        query
            .select(cb.count(root))
            .where(cb.equal(root.get(AbstractJudgelsModel_.jid), clientJid));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public long countByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<GraderClientModel> root = query.from(GraderClientModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("name"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(cb.count(root))
                .where(condition);

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<GraderClientModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<GraderClientModel> query = cb.createQuery(GraderClientModel.class);
        Root<GraderClientModel> root = query.from(GraderClientModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("name"), "%" + filterString + "%"));

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