package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingSubmissionModel;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public final class ProgrammingSubmissionHibernateDao extends AbstractJudgelsHibernateDao<ProgrammingSubmissionModel> implements ProgrammingSubmissionDao {
    @Override
    public List<ProgrammingSubmissionModel> findByProblem(String problemJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ProgrammingSubmissionModel> query = cb.createQuery(ProgrammingSubmissionModel.class);

        Root<ProgrammingSubmissionModel> root = query.from(ProgrammingSubmissionModel.class);

        query = query.where(cb.equal(root.get("problemJid"), problemJid));

        return JPA.em().createQuery(query).getResultList();
    }
}
