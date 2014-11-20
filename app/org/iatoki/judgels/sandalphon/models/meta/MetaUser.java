package org.iatoki.judgels.sandalphon.models.meta;

import org.iatoki.judgels.commons.models.meta.MetaAbstractUser;
import org.iatoki.judgels.sandalphon.models.schema.User;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(User.class)
public abstract class MetaUser extends MetaAbstractUser {

    public static volatile SingularAttribute<User, String> userId;

}

