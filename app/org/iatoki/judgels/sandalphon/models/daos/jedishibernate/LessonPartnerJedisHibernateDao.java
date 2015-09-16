package org.iatoki.judgels.sandalphon.models.daos.jedishibernate;

import org.iatoki.judgels.play.models.daos.impls.AbstractJedisHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonPartnerDao;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel_;
import play.db.jpa.JPA;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Singleton
@Named("lessonPartnerDao")
public final class LessonPartnerJedisHibernateDao extends AbstractJedisHibernateDao<Long, LessonPartnerModel> implements LessonPartnerDao {

    @Inject
    public LessonPartnerJedisHibernateDao(JedisPool jedisPool) {
        super(jedisPool, LessonPartnerModel.class);
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
    public List<String> getLessonJidsByPartnerJid(String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<LessonPartnerModel> root = query.from(getModelClass());

        query
                .select(root.get(LessonPartnerModel_.lessonJid))
                .where(cb.equal(root.get(LessonPartnerModel_.userJid), partnerJid));

        return JPA.em().createQuery(query).getResultList();
    }
}
