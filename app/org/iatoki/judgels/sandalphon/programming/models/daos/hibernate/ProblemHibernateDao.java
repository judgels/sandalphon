package org.iatoki.judgels.sandalphon.programming.models.daos.hibernate;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemModel;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemModel_;

import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

public final class ProblemHibernateDao extends AbstractJudgelsHibernateDao<ProblemModel> implements ProblemDao {

    public ProblemHibernateDao() {
        super(ProblemModel.class);
    }

    @Override
    protected List<SingularAttribute<ProblemModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(ProblemModel_.name);
    }
}