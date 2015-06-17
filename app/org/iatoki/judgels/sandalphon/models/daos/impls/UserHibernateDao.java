package org.iatoki.judgels.sandalphon.models.daos.impls;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.UserDao;
import org.iatoki.judgels.sandalphon.models.entities.UserModel;
import org.iatoki.judgels.sandalphon.models.entities.UserModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.List;

public final class UserHibernateDao extends AbstractHibernateDao<Long, UserModel> implements UserDao {

    public UserHibernateDao() {
        super(UserModel.class);
    }

    @Override
    public boolean existsByUserJid(String userJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UserModel> root = query.from(UserModel.class);

        query
            .select(cb.count(root))
            .where(cb.equal(root.get(UserModel_.userJid), userJid));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public UserModel findByUserJid(String userJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<UserModel> query = cb.createQuery(UserModel.class);
        Root<UserModel> root = query.from(UserModel.class);

        query.where(cb.equal(root.get(UserModel_.userJid), userJid));

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<String> findUserJidByFilter(String filterString) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserModel> root = query.from(UserModel.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get(UserModel_.userJid), "%" + filterString + "%"));

        Predicate condition = cb.or(predicates.toArray(new Predicate[predicates.size()]));

        query
                .select(root.get(UserModel_.userJid))
                .where(condition);

        return JPA.em().createQuery(query).getResultList();
    }

    @Override
    protected List<SingularAttribute<UserModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(UserModel_.roles);
    }
}
