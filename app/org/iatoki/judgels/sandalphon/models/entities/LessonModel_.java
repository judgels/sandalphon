package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.play.models.entities.AbstractJudgelsModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(LessonModel.class)
public abstract class LessonModel_ extends AbstractJudgelsModel_ {

    public static volatile SingularAttribute<LessonModel, String> name;
    public static volatile SingularAttribute<LessonModel, String> additionalNote;
}