package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ProgrammingProblemModel.class)
public abstract class ProgrammingProblemModel_ extends AbstractJudgelsModel_ {

    public static volatile SingularAttribute<ProgrammingProblemModel, String> gradingEngine;
    public static volatile SingularAttribute<ProgrammingProblemModel, String> additionalNote;
    public static volatile SingularAttribute<ProgrammingProblemModel, String> languageRestriction;
}