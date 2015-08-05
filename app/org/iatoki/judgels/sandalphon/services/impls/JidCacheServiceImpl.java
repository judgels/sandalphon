package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.play.services.impls.AbstractBaseJidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.models.daos.JidCacheDao;
import org.iatoki.judgels.sandalphon.models.entities.JidCacheModel;
import org.iatoki.judgels.sandalphon.services.JidCacheService;

public final class JidCacheServiceImpl extends AbstractBaseJidCacheServiceImpl<JidCacheModel> implements JidCacheService {

    private static JidCacheServiceImpl INSTANCE;

    private JidCacheServiceImpl(JidCacheDao jidCacheDao) {
        super(jidCacheDao);
    }

    public static synchronized void buildInstance(JidCacheDao jidCacheDao) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("JidCacheService instance has already been built");
        }
        INSTANCE = new JidCacheServiceImpl(jidCacheDao);
    }

    public static JidCacheServiceImpl getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("JidCacheService instance has not been built");
        }
        return INSTANCE;
    }
}
