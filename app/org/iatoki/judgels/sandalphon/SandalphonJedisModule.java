package org.iatoki.judgels.sandalphon;

public final class SandalphonJedisModule extends SandalphonModule {

    @Override
    protected String getDaosImplPackage() {
        return "org.iatoki.judgels.sandalphon.models.daos.jedishibernate";
    }

}