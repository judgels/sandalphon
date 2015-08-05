package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.play.services.impls.AbstractBaseDataMigrationServiceImpl;

import java.sql.SQLException;

public final class SandalphonDataMigrationServiceImpl extends AbstractBaseDataMigrationServiceImpl {

    @Override
    protected void onUpgrade(long databaseVersion, long codeDatabaseVersion) throws SQLException {

    }

    @Override
    public long getCodeDataVersion() {
        return 1;
    }
}
