package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public final class ProblemHibernateDao extends AbstractJudgelsHibernateDao<ProblemModel> implements ProblemDao {
    public ProblemHibernateDao() {
        super(ProblemModel.class);
    }

    @Override
    public List<String> findProblemJidsByAuthorJid(String authorJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ProblemModel> root = query.from(getModelClass());

        query
                .select(root.get(ProblemModel_.jid))
                .where(cb.equal(root.get(ProblemModel_.userCreate), authorJid));

        return JPA.em().createQuery(query).getResultList();
    }
}
