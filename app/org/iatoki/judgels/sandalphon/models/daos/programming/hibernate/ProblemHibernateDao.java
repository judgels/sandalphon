package org.iatoki.judgels.sandalphon.models.daos.programming.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel_;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public final class ProblemHibernateDao extends AbstractJudgelsHibernateDao<ProblemModel> implements ProblemDao {

    @Override
    public boolean isProblemExistByProblemJid(String problemJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ProblemModel> root = query.from(ProblemModel.class);

        query
                .select(cb.count(root))
                .where(cb.equal(root.get(AbstractJudgelsModel_.jid), problemJid));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public long countByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ProblemModel> root = query.from(ProblemModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("name"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(cb.count(root))
                .where(condition);

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<ProblemModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProblemModel> query = cb.createQuery(ProblemModel.class);
        Root<ProblemModel> root = query.from(ProblemModel.class);

        List<Selection<?>> selection = new ArrayList<>();
        selection.add(root.get("id"));
        selection.add(root.get("jid"));
        selection.add(root.get("name"));
        selection.add(root.get("gradingEngine"));
        selection.add(root.get("additionalNote"));

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
                .multiselect(selection)
                .where(condition)
                .orderBy(orderBy);

        return JPA.em().createQuery(query).setFirstResult((int) first).setMaxResults((int) max).getResultList();
    }
}