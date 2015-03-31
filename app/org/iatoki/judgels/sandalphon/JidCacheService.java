package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.AbstractJidCacheService;
import org.iatoki.judgels.sandalphon.models.domains.JidCacheModel;

public final class JidCacheService extends AbstractJidCacheService<JidCacheModel> {
    private static JidCacheService INSTANCE;

    private JidCacheService() {

    }

    public static JidCacheService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JidCacheService();
        }
        return INSTANCE;
    }
}
