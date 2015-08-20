package org.iatoki.judgels.sandalphon.services.impls;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.iatoki.judgels.play.services.impls.AbstractBaseDataMigrationServiceImpl;
import play.db.jpa.JPA;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SandalphonDataMigrationServiceImpl extends AbstractBaseDataMigrationServiceImpl {

    @Override
    public long getCodeDataVersion() {
        return 2;
    }

    @Override
    protected void onUpgrade(long databaseVersion, long codeDatabaseVersion) throws SQLException {
        if (databaseVersion < 2) {
            migrateV1toV2();
        }
    }

    private void migrateV1toV2() throws SQLException {
        SessionImpl session = (SessionImpl) JPA.em().unwrap(Session.class);
        Connection connection = session.getJdbcConnectionAccess().obtainConnection();

        String programmingSubmissionTable = "sandalphon_submission_programming";
        String newProgrammingSubmissionTable = "sandalphon_programming_submission";
        String bundleSubmissionTable = "sandalphon_submission_bundle";
        String newBundleSubmissionTable = "sandalphon_bundle_submission";
        Statement statement = connection.createStatement();

        statement.execute("ALTER TABLE " + programmingSubmissionTable + " CHANGE contestJid containerJid VARCHAR(255);");

        statement.execute("DROP TABLE " + newProgrammingSubmissionTable + ";");
        statement.execute("DROP TABLE " + newBundleSubmissionTable + ";");

        statement.execute("RENAME TABLE " + programmingSubmissionTable + " TO " + newProgrammingSubmissionTable + ";");
        statement.execute("RENAME TABLE " + bundleSubmissionTable + " TO " + newBundleSubmissionTable + ";");
    }
}
