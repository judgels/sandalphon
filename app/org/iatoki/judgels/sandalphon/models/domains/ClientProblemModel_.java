package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel_;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ClientProblemModel.class)
public abstract class ClientProblemModel_ extends AbstractModel_ {

	public static volatile SingularAttribute<ClientProblemModel, Long> id;
	public static volatile SingularAttribute<ClientProblemModel, String> clientJid;
	public static volatile SingularAttribute<ClientProblemModel, String> problemJid;
	public static volatile SingularAttribute<ClientProblemModel, String> secret;
}

