package org.iatoki.judgels.sandalphon.models.entities.programming;

import org.iatoki.judgels.play.models.domains.AbstractJudgelsModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(GraderModel.class)
public abstract class GraderModel_ extends AbstractJudgelsModel_ {
        public static volatile SingularAttribute<GraderModel, String> name;
        public static volatile SingularAttribute<GraderModel, String> secret;
}
