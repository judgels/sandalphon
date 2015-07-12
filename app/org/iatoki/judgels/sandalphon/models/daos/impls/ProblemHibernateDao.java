package org.iatoki.judgels.sandalphon.models.daos.impls;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.models.daos.impls.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.ProblemDao;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel_;
import play.db.jpa.JPA;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

@Singleton
@Named("problemDao")
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

    @Override
    protected List<SingularAttribute<ProblemModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(ProblemModel_.name, ProblemModel_.additionalNote);
    }
}
