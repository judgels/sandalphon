package org.iatoki.judgels.sandalphon.models.daos.impls;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonDao;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel_;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

public final class LessonHibernateDao extends AbstractJudgelsHibernateDao<LessonModel> implements LessonDao {
    public LessonHibernateDao() {
        super(LessonModel.class);
    }

    @Override
    public List<String> findLessonJidsByAuthorJid(String authorJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<LessonModel> root = query.from(getModelClass());

        query
                .select(root.get(ProblemModel_.jid))
                .where(cb.equal(root.get(LessonModel_.userCreate), authorJid));

        return JPA.em().createQuery(query).getResultList();
    }

    @Override
    protected List<SingularAttribute<LessonModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(LessonModel_.name, LessonModel_.additionalNote);
    }
}
