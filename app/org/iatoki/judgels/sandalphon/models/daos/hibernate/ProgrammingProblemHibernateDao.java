package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblemModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public final class ProgrammingProblemHibernateDao extends AbstractJudgelsHibernateDao<ProgrammingProblemModel> implements ProgrammingProblemDao {
    @Override
    public long countByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ProgrammingProblemModel> root = query.from(ProgrammingProblemModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("name"), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(cb.count(root))
                .where(condition);

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<ProgrammingProblemModel> findByFilterAndSort(String filterString, String sortBy, String order, long first, long max) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProgrammingProblemModel> query = cb.createQuery(ProgrammingProblemModel.class);
        Root<ProgrammingProblemModel> root = query.from(ProgrammingProblemModel.class);

        List<Selection<?>> selection = new ArrayList<>();
        selection.add(root.get("id"));
        selection.add(root.get("jid"));
        selection.add(root.get("name"));
        selection.add(root.get("gradingMethod"));
        selection.add(root.get("note"));

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