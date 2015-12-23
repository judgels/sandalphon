package org.iatoki.judgels.sandalphon.config;

import org.iatoki.judgels.sandalphon.SandalphonModule;

public final class SandalphonJedisModule extends SandalphonModule {

    @Override
    protected String getDaosImplPackage() {
        return "org.iatoki.judgels.sandalphon.models.daos.jedishibernate";
    }

}
