package org.iatoki.judgels.sandalphon.models.daos.impls;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJidCacheHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.JidCacheDao;
import org.iatoki.judgels.sandalphon.models.entities.JidCacheModel;

public final class JidCacheHibernateDao extends AbstractJidCacheHibernateDao<JidCacheModel> implements JidCacheDao {
    public JidCacheHibernateDao() {
        super(JidCacheModel.class);
    }

    @Override
    public JidCacheModel createJidCacheModel() {
        return new JidCacheModel();
    }
}
