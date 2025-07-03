package org.sqlite;

/**
 * <a
 * href="https://www.sqlite.org/c3ref/update_hook.html">https://www.sqlite.org/c3ref/update_hook.html</a>
 */
public interface SQLiteUpdateListener {

    enum Type {
        INSERT,
        DELETE,
        UPDATE
    }

    void onUpdate(Type type, String database, String table, long rowId);
}
