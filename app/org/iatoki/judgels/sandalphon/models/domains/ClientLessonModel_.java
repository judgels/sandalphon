package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ClientLessonModel.class)
public abstract class ClientLessonModel_ extends AbstractModel_ {

	public static volatile SingularAttribute<ClientLessonModel, Long> id;
	public static volatile SingularAttribute<ClientLessonModel, String> clientJid;
	public static volatile SingularAttribute<ClientLessonModel, String> lessonJid;
	public static volatile SingularAttribute<ClientLessonModel, String> secret;
}

