package org.iatoki.judgels.sandalphon.models.dao.hibernate;

import org.iatoki.judgels.commons.models.dao.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.dao.interfaces.UserDao;
import org.iatoki.judgels.sandalphon.models.schema.User;

public final class UserHibernateDao extends AbstractHibernateDao<String, User>
        implements UserDao {

}
