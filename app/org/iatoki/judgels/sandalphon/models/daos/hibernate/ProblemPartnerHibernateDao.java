package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public final class ProblemPartnerHibernateDao extends AbstractHibernateDao<Long, ProblemPartnerModel> implements ProblemPartnerDao {
    public ProblemPartnerHibernateDao() {
        super(ProblemPartnerModel.class);
    }

    @Override
    public boolean existsByProblemJidAndPartnerJid(String problemJid, String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ProblemPartnerModel> root = query.from(getModelClass());

        query
                .select(cb.count(root))
                .where(cb.and(cb.equal(root.get(ProblemPartnerModel_.problemJid), problemJid), cb.equal(root.get(ProblemPartnerModel_.userJid), partnerJid)));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public ProblemPartnerModel findByProblemJidAndPartnerJid(String problemJid, String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProblemPartnerModel> query = cb.createQuery(getModelClass());
        Root<ProblemPartnerModel> root = query.from(getModelClass());

        query
                .where(cb.and(cb.equal(root.get(ProblemPartnerModel_.problemJid), problemJid), cb.equal(root.get(ProblemPartnerModel_.userJid), partnerJid)));

        return JPA.em().createQuery(query).getSingleResult();
    }

    @Override
    public List<String> findProblemJidsByPartnerJid(String partnerJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ProblemPartnerModel> root = query.from(getModelClass());

        query
                .select(root.get(ProblemPartnerModel_.problemJid))
                .where(cb.equal(root.get(ProblemPartnerModel_.userJid), partnerJid));

        return JPA.em().createQuery(query).getResultList();
    }
}
