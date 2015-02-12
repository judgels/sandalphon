package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserRoleDao;
import org.iatoki.judgels.sandalphon.models.domains.UserRoleModel;
import org.iatoki.judgels.sandalphon.models.domains.UserRoleModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.List;

public final class UserRoleHibernateDao extends AbstractHibernateDao<Long, UserRoleModel> implements UserRoleDao {

    public UserRoleHibernateDao() {
        super(UserRoleModel.class);
    }

    @Override
    public boolean existsByUserJid(String userJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UserRoleModel> root = query.from(UserRoleModel.class);

        query
            .select(cb.count(root))
            .where(cb.equal(root.get(UserRoleModel_.userJid), userJid));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public UserRoleModel findByUserJid(String userJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<UserRoleModel> query = cb.createQuery(UserRoleModel.class);
        Root<UserRoleModel> root = query.from(UserRoleModel.class);

        query.where(cb.equal(root.get(UserRoleModel_.userJid), userJid));

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<String> findUserJidByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserRoleModel> root = query.from(UserRoleModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get(UserRoleModel_.userJid), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(root.get(UserRoleModel_.userJid))
                .where(condition);

        return JPA.em().createQuery(query).getResultList();
    }

    @Override
    protected List<SingularAttribute<UserRoleModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(UserRoleModel_.userJid);
    }
}
