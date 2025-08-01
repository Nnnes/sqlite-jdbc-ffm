// --------------------------------------
// sqlite-jdbc Project
//
// InsertQueryTest.java
// Since: Apr 7, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InsertQueryTest {
    String dbName;

    @BeforeEach
    public void setUp(@TempDir File tempDir) throws Exception {
        File tmpFile = File.createTempFile("tmp-sqlite", ".db", tempDir);
        dbName = tmpFile.getAbsolutePath();
    }

    interface ConnectionFactory {
        Connection getConnection() throws SQLException;

        void dispose() throws SQLException;
    }

    class IndependentConnectionFactory implements ConnectionFactory {
        // Internal bookkeeping so connections are still closed in the end.
        private final List<Connection> connections = new ArrayList<>();

        public Connection getConnection() throws SQLException {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            connections.add(conn);
            return conn;
        }

        public void dispose() throws SQLException {
            for (Connection connection : connections) {
                connection.close();
            }
            connections.clear();
        }
    }

    class SharedConnectionFactory implements ConnectionFactory {
        private Connection conn = null;

        public Connection getConnection() throws SQLException {
            if (conn == null) {
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            }
            return conn;
        }

        public void dispose() throws SQLException {
            if (conn != null) {
                conn.close();
            }
        }
    }

    static class BD {
        String fullId;
        String type;

        public BD(String fullId, String type) {
            this.fullId = fullId;
            this.type = type;
        }

        public String getFullId() {
            return fullId;
        }

        public void setFullId(String fullId) {
            this.fullId = fullId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public static byte[] serializeBD(BD item) {
            return new byte[0];
        }
    }

    @Test
    public void insertLockTestUsingSharedConnection() throws Exception {
        insertAndQuery(new SharedConnectionFactory());
    }

    @Test
    public void insertLockTestUsingIndependentConnection() throws Exception {
        insertAndQuery(new IndependentConnectionFactory());
    }

    void insertAndQuery(ConnectionFactory factory) throws SQLException {
        try {
            Statement st = factory.getConnection().createStatement();
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS data (fid VARCHAR(255) PRIMARY KEY, type VARCHAR(64), data BLOB);");
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS ResourcesTags (bd_fid VARCHAR(255), name VARCHAR(64), version INTEGER);");
            st.close();

            factory.getConnection().setAutoCommit(false);

            // Object Serialization
            PreparedStatement statAddBD =
                    factory.getConnection()
                            .prepareStatement("INSERT OR REPLACE INTO data values (?, ?, ?)");
            PreparedStatement statDelRT =
                    factory.getConnection()
                            .prepareStatement("DELETE FROM ResourcesTags WHERE bd_fid = ?");
            PreparedStatement statAddRT =
                    factory.getConnection()
                            .prepareStatement("INSERT INTO ResourcesTags values (?, ?, ?)");

            for (int i = 0; i < 10; i++) {
                BD item = new BD(Integer.toHexString(i), Integer.toString(i));

                // SQLite database insertion
                statAddBD.setString(1, item.getFullId());
                statAddBD.setString(2, item.getType());
                statAddBD.setBytes(3, BD.serializeBD(item));
                statAddBD.execute();

                // Then, its resources tags
                statDelRT.setString(1, item.getFullId());
                statDelRT.execute();

                statAddRT.setString(1, item.getFullId());

                for (int j = 0; j < 2; j++) {
                    statAddRT.setString(2, "1");
                    statAddRT.setLong(3, 1L);
                    statAddRT.execute();
                }
            }

            factory.getConnection().setAutoCommit(true);

            statAddBD.close();
            statDelRT.close();
            statAddRT.close();

            //
            PreparedStatement stat;
            String query = "SELECT COUNT(fid) FROM data";

            stat = factory.getConnection().prepareStatement(query);
            ResultSet rs = stat.executeQuery();

            rs.next();
            // System.out.println("count = " + rs.getLong(1));

            rs.close();
            stat.close();
        } finally {
            factory.dispose();
        }
    }

    @Disabled("Not sure this worked recently, the second query cannot find the table 'sample'")
    @Test
    public void reproduceDatabaseLocked() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        Connection conn2 = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        Statement stat = conn.createStatement();
        Statement stat2 = conn2.createStatement();

        conn.setAutoCommit(false);

        stat.executeUpdate("drop table if exists sample");
        stat.executeUpdate("create table sample(id, name)");
        stat.executeUpdate("insert into sample values(1, 'leo')");

        ResultSet rs = stat2.executeQuery("select count(*) from sample");
        rs.next();

        conn.commit(); // causes "database is locked" (SQLITE_BUSY)
    }
}
