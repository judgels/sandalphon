package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.LessonPartnerDao;
import org.iatoki.judgels.sandalphon.models.domains.LessonPartnerModel;
import org.iatoki.judgels.sandalphon.models.domains.LessonPartnerModel_;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public final class LessonPartnerHibernateDao extends AbstractHibernateDao<Long, LessonPartnerModel> implements LessonPartnerDao {
    public LessonPartnerHibernateDao() {
        super(LessonPartnerModel.class);
    }

    @Override
    public boolean existsByLessonJidAndPartnerJid(String lessonJid, String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<LessonPartnerModel> root = query.from(getModelClass());

        query
                .select(cb.count(root))
                .where(cb.and(cb.equal(root.get(LessonPartnerModel_.lessonJid), lessonJid), cb.equal(root.get(LessonPartnerModel_.userJid), partnerJid)));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public LessonPartnerModel findByLessonJidAndPartnerJid(String lessonJid, String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<LessonPartnerModel> query = cb.createQuery(getModelClass());
        Root<LessonPartnerModel> root = query.from(getModelClass());

        query
                .where(cb.and(cb.equal(root.get(LessonPartnerModel_.lessonJid), lessonJid), cb.equal(root.get(LessonPartnerModel_.userJid), partnerJid)));

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<String> findLessonJidsByPartnerJid(String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<LessonPartnerModel> root = query.from(getModelClass());

        query
                .select(root.get(LessonPartnerModel_.lessonJid))
                .where(cb.equal(root.get(LessonPartnerModel_.userJid), partnerJid));

        return JPA.em().createQuery(query).getResultList();
    }
}
