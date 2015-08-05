package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.play.models.entities.AbstractJudgelsModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ProblemModel.class)
public abstract class ProblemModel_ extends AbstractJudgelsModel_ {

    public static volatile SingularAttribute<ProblemModel, String> name;
    public static volatile SingularAttribute<ProblemModel, String> additionalNote;
}