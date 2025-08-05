/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
// --------------------------------------
// SQLite JDBC Project
//
// SQLite.java
// Since: 2007/05/10
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite library version methods
 */
public class SQLiteJDBCLoader {
    private static final String VERSION;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;

    static {
        String version = "unknown";
        try (SQLiteConnection conn =
                     (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            version = conn.libversion();
        } catch (SQLException _) {
        }
        VERSION = version;
        String[] c = VERSION.split("\\.");
        MAJOR_VERSION = (c.length > 0) ? Integer.parseInt(c[0]) : 1;
        MINOR_VERSION = (c.length > 1) ? Integer.parseInt(c[1]) : 0;
    }

    /**
     * @return The major version of the SQLite JDBC driver.
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * @return The minor version of the SQLite JDBC driver.
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * @return The version of the SQLite JDBC driver.
     */
    public static String getVersion() {
        return VERSION;
    }
}
