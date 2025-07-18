/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.sqlite.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import org.sqlite.BusyHandler;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.util.Logger;
import org.sqlite.util.LoggerFactory;

/// This class interfaces with [NativeDB_c] in the same way as it does with `NativeDB.c` in the
/// original JNI version of `sqlite-jdbc`.
///
/// Conversions between [String]s and UTF-8 `byte[]`s have been replaced with built-in FFM methods
/// such as [Arena#allocateFrom(java.lang.String)] and [MemorySegment#getString(long,
/// java.nio.charset.Charset)].
public final class NativeDB extends DB {
    private static final Logger logger = LoggerFactory.getLogger(NativeDB.class);
    private static final int DEFAULT_BACKUP_BUSY_SLEEP_TIME_MILLIS = 100;
    private static final int DEFAULT_BACKUP_NUM_BUSY_BEFORE_FAIL = 3;
    private static final int DEFAULT_PAGES_PER_BACKUP_STEP = 100;

    private final NativeDB_c $this;

    private static boolean isLoaded;
    private static boolean loadSucceeded;

    static {
        if ("The Android Project".equals(System.getProperty("java.vm.vendor"))) {
            System.loadLibrary("sqlitejdbc");
            isLoaded = true;
            loadSucceeded = true;
        } else {
            // continue with non Android execution path
            isLoaded = false;
            loadSucceeded = false;
        }
    }

    public NativeDB(String url, String fileName, SQLiteConfig config) throws SQLException {
        super(url, fileName, config);
        $this = new NativeDB_c(this);
    }

    /**
     * Loads the SQLite interface backend.
     *
     * @return True if the SQLite JDBC driver is successfully loaded; false otherwise.
     */
    public static boolean load() {
        if (isLoaded) return loadSucceeded;

        try {
            // TODO: Rewrite SQLiteJDBCLoader instead of relying on the library being loaded in
            //  sqlite_h
            // loadSucceeded = SQLiteJDBCLoader.initialize();
            System.loadLibrary("sqlite3");
            loadSucceeded = true;
        } finally {
            isLoaded = true;
        }
        return loadSucceeded;
    }

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    /**
     * @see org.sqlite.core.DB#_open(java.lang.String, int)
     */
    @Override
    protected synchronized void _open(String file, int openFlags) throws SQLException {
        $this._open(file, openFlags);
    }

    /**
     * @see org.sqlite.core.DB#_close()
     */
    @Override
    protected synchronized void _close() throws SQLException {
        $this._close();
    }

    /**
     * @see org.sqlite.core.DB#_exec(java.lang.String)
     */
    @Override
    public synchronized int _exec(String sql) throws SQLException {
        logger.trace(
                () ->
                        MessageFormat.format(
                                "DriverManager [{0}] [SQLite EXEC] {1}",
                                Thread.currentThread().getName(), sql));
        return $this._exec(sql);
    }

    /**
     * @see org.sqlite.core.DB#shared_cache(boolean)
     */
    @Override
    public synchronized int shared_cache(boolean enable) {
        return $this.shared_cache(enable);
    }

    /**
     * @see org.sqlite.core.DB#enable_load_extension(boolean)
     */
    @Override
    public synchronized int enable_load_extension(boolean enable) {
        try {
            return $this.enable_load_extension(enable);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#interrupt()
     */
    @Override
    public void interrupt() {
        try {
            $this.interrupt();
        } catch (SQLException _) {
        }
    }

    /**
     * @see org.sqlite.core.DB#busy_timeout(int)
     */
    @Override
    public synchronized void busy_timeout(int ms) {
        try {
            $this.busy_timeout(ms);
        } catch (SQLException _) {
        }
    }

    Arena busyHandlerArena = null;

    /**
     * @see org.sqlite.core.DB#busy_handler(BusyHandler)
     */
    @Override
    public synchronized void busy_handler(BusyHandler busyHandler) {
        try {
            $this.busy_handler(busyHandler);
        } catch (SQLException _) {
        }
    }

    /**
     * @see org.sqlite.core.DB#prepare(java.lang.String)
     */
    @Override
    protected synchronized SafeStmtPtr prepare(String sql) throws SQLException {
        logger.trace(
                () ->
                        MessageFormat.format(
                                "DriverManager [{0}] [SQLite EXEC] {1}",
                                Thread.currentThread().getName(), sql));
        return new SafeStmtPtr(this, $this.prepare(sql));
    }

    /**
     * @see org.sqlite.core.DB#errmsg()
     */
    @Override
    synchronized String errmsg() {
        try {
            return $this.errmsg();
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#libversion()
     */
    @Override
    public synchronized String libversion() {
        return $this.libversion();
    }

    /**
     * @see org.sqlite.core.DB#changes()
     */
    @Override
    public synchronized long changes() {
        try {
            return $this.changes();
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#total_changes()
     */
    @Override
    public synchronized long total_changes() {
        try {
            return $this.total_changes();
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#finalize(MemorySegment)
     */
    @Override
    protected synchronized int finalize(MemorySegment stmt) {
        try {
            return $this.finalize(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#step(MemorySegment)
     */
    @Override
    public synchronized int step(MemorySegment stmt) {
        try {
            return $this.step(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#reset(MemorySegment)
     */
    @Override
    public synchronized int reset(MemorySegment stmt) {
        try {
            return $this.reset(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#clear_bindings(MemorySegment)
     */
    @Override
    public synchronized int clear_bindings(MemorySegment stmt) {
        try {
            return $this.clear_bindings(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_parameter_count(MemorySegment)
     */
    @Override
    synchronized int bind_parameter_count(MemorySegment stmt) {
        try {
            return $this.bind_parameter_count(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_count(MemorySegment)
     */
    @Override
    public synchronized int column_count(MemorySegment stmt) {
        try {
            return $this.column_count(stmt);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_type(MemorySegment, int)
     */
    @Override
    public synchronized int column_type(MemorySegment stmt, int col) {
        try {
            return $this.column_type(stmt, col);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_decltype(MemorySegment, int)
     */
    @Override
    public synchronized String column_decltype(MemorySegment stmt, int col) {
        try {
            return $this.column_decltype(stmt, col);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_table_name(MemorySegment, int)
     */
    @Override
    public synchronized String column_table_name(MemorySegment stmt, int col) {
        try {
            return $this.column_table_name(stmt, col);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_name(MemorySegment, int)
     */
    @Override
    public synchronized String column_name(MemorySegment stmt, int col) {
        try {
            return $this.column_name(stmt, col);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_text(MemorySegment, int)
     */
    @Override
    public synchronized String column_text(MemorySegment stmt, int col) {
        try {
            return $this.column_text(stmt, col);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_blob(MemorySegment, int)
     */
    @Override
    public synchronized byte[] column_blob(MemorySegment stmt, int col) {
        try {
            return $this.column_blob(stmt, col);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_double(MemorySegment, int)
     */
    @Override
    public synchronized double column_double(MemorySegment stmt, int col) {
        try {
            return $this.column_double(stmt, col);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_long(MemorySegment, int)
     */
    @Override
    public synchronized long column_long(MemorySegment stmt, int col) {
        try {
            return $this.column_long(stmt, col);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#column_int(MemorySegment, int)
     */
    @Override
    public synchronized int column_int(MemorySegment stmt, int col) {
        try {
            return $this.column_int(stmt, col);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_null(MemorySegment, int)
     */
    @Override
    synchronized int bind_null(MemorySegment stmt, int pos) {
        try {
            return $this.bind_null(stmt, pos);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_int(MemorySegment, int, int)
     */
    @Override
    synchronized int bind_int(MemorySegment stmt, int pos, int v) {
        try {
            return $this.bind_int(stmt, pos, v);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_long(MemorySegment, int, long)
     */
    @Override
    synchronized int bind_long(MemorySegment stmt, int pos, long v) {
        try {
            return $this.bind_long(stmt, pos, v);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_double(MemorySegment, int, double)
     */
    @Override
    synchronized int bind_double(MemorySegment stmt, int pos, double v) {
        try {
            return $this.bind_double(stmt, pos, v);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_text(MemorySegment, int, java.lang.String)
     */
    @Override
    synchronized int bind_text(MemorySegment stmt, int pos, String v) {
        try {
            return $this.bind_text(stmt, pos, v);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#bind_blob(MemorySegment, int, byte[])
     */
    @Override
    synchronized int bind_blob(MemorySegment stmt, int pos, byte[] v) {
        try {
            return $this.bind_blob(stmt, pos, v);
        } catch (SQLException _) {
            return SQLITE_MISUSE;
        }
    }

    /**
     * @see org.sqlite.core.DB#result_null(MemorySegment)
     */
    @Override
    public synchronized void result_null(MemorySegment context) {
        $this.result_null(context);
    }

    /**
     * @see org.sqlite.core.DB#result_text(MemorySegment, java.lang.String)
     */
    @Override
    public synchronized void result_text(MemorySegment context, String val) {
        $this.result_text(context, val);
    }

    /**
     * @see org.sqlite.core.DB#result_blob(MemorySegment, byte[])
     */
    @Override
    public synchronized void result_blob(MemorySegment context, byte[] val) {
        $this.result_blob(context, val);
    }

    /**
     * @see org.sqlite.core.DB#result_double(MemorySegment, double)
     */
    @Override
    public synchronized void result_double(MemorySegment context, double val) {
        $this.result_double(context, val);
    }

    /**
     * @see org.sqlite.core.DB#result_long(MemorySegment, long)
     */
    @Override
    public synchronized void result_long(MemorySegment context, long val) {
        $this.result_long(context, val);
    }

    /**
     * @see org.sqlite.core.DB#result_int(MemorySegment, int)
     */
    @Override
    public synchronized void result_int(MemorySegment context, int val) {
        $this.result_int(context, val);
    }

    /**
     * @see org.sqlite.core.DB#result_error(MemorySegment, java.lang.String)
     */
    @Override
    public synchronized void result_error(MemorySegment context, String err) {
        $this.result_error(context, err);
    }

    /**
     * @see org.sqlite.core.DB#value_text(org.sqlite.Function, int)
     */
    @Override
    public synchronized String value_text(Function f, int arg) {
        try {
            return $this.value_text(f, arg);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#value_blob(org.sqlite.Function, int)
     */
    @Override
    public synchronized byte[] value_blob(Function f, int arg) {
        try {
            return $this.value_blob(f, arg);
        } catch (SQLException _) {
            return null;
        }
    }

    /**
     * @see org.sqlite.core.DB#value_double(org.sqlite.Function, int)
     */
    @Override
    public synchronized double value_double(Function f, int arg) {
        try {
            return $this.value_double(f, arg);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#value_long(org.sqlite.Function, int)
     */
    @Override
    public synchronized long value_long(Function f, int arg) {
        try {
            return $this.value_long(f, arg);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#value_int(org.sqlite.Function, int)
     */
    @Override
    public synchronized int value_int(Function f, int arg) {
        try {
            return $this.value_int(f, arg);
        } catch (SQLException _) {
            return 0;
        }
    }

    /**
     * @see org.sqlite.core.DB#value_type(org.sqlite.Function, int)
     */
    @Override
    public synchronized int value_type(Function f, int arg) {
        try {
            return $this.value_type(f, arg);
        } catch (SQLException e) {
            // Likely caused a JVM crash in sqlite-jdbc. See NativeDB_c#value_type(Function, int)
            return -1;
        }
    }

    /**
     * @see org.sqlite.core.DB#create_function(java.lang.String, org.sqlite.Function, int, int)
     */
    @Override
    public synchronized int create_function(String name, Function func, int nArgs, int flags)
            throws SQLException {
        return $this.create_function(validateName("function", name), func, nArgs, flags);
    }

    /**
     * @see org.sqlite.core.DB#destroy_function(java.lang.String)
     */
    @Override
    public synchronized int destroy_function(String name) throws SQLException {
        return $this.destroy_function(validateName("function", name));
    }

    /**
     * @see org.sqlite.core.DB#create_collation(String, Collation)
     */
    @Override
    public synchronized int create_collation(String name, Collation coll) throws SQLException {
        return $this.create_collation(validateName("collation", name), coll);
    }

    /**
     * @see org.sqlite.core.DB#destroy_collation(String)
     */
    @Override
    public synchronized int destroy_collation(String name) throws SQLException {
        return $this.destroy_collation(validateName("collation", name));
    }

    @Override
    public synchronized int limit(int id, int value) throws SQLException {
        return $this.limit(id, value);
    }

    private String validateName(String nameType, String name) throws SQLException {
        if (name == null || name.isEmpty() || name.getBytes(StandardCharsets.UTF_8).length > 255) {
            throw new SQLException("invalid " + nameType + " name: '" + name + "'");
        }
        return name;
    }

    /**
     * @see org.sqlite.core.DB#backup(java.lang.String, java.lang.String,
     *     org.sqlite.core.DB.ProgressObserver)
     */
    @Override
    public int backup(String dbName, String destFileName, ProgressObserver observer)
            throws SQLException {
        return $this.backup(
                dbName,
                destFileName,
                observer,
                DEFAULT_BACKUP_BUSY_SLEEP_TIME_MILLIS,
                DEFAULT_BACKUP_NUM_BUSY_BEFORE_FAIL,
                DEFAULT_PAGES_PER_BACKUP_STEP);
    }

    /**
     * @see org.sqlite.core.DB#backup(String, String, org.sqlite.core.DB.ProgressObserver, int, int,
     *     int)
     */
    @Override
    public int backup(
            String dbName,
            String destFileName,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeouts,
            int pagesPerStep)
            throws SQLException {
        return $this.backup(
                dbName, destFileName, observer, sleepTimeMillis, nTimeouts, pagesPerStep);
    }

    /**
     * @see org.sqlite.core.DB#restore(java.lang.String, java.lang.String,
     *     org.sqlite.core.DB.ProgressObserver)
     */
    @Override
    public synchronized int restore(String dbName, String sourceFileName, ProgressObserver observer)
            throws SQLException {

        return $this.restore(
                dbName,
                sourceFileName,
                observer,
                DEFAULT_BACKUP_BUSY_SLEEP_TIME_MILLIS,
                DEFAULT_BACKUP_NUM_BUSY_BEFORE_FAIL,
                DEFAULT_PAGES_PER_BACKUP_STEP);
    }

    /**
     * @see org.sqlite.core.DB#restore(String, String, ProgressObserver, int, int, int)
     */
    @Override
    public synchronized int restore(
            String dbName,
            String sourceFileName,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeouts,
            int pagesPerStep)
            throws SQLException {

        return $this.restore(
                dbName, sourceFileName, observer, sleepTimeMillis, nTimeouts, pagesPerStep);
    }

    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    /**
     * Provides metadata for table columns.
     *
     * @returns For each column returns: <br>
     *     res[col][0] = true if column constrained NOT NULL<br>
     *     res[col][1] = true if column is part of the primary key<br>
     *     res[col][2] = true if column is auto-increment.
     * @see org.sqlite.core.DB#column_metadata(MemorySegment)
     */
    @Override
    synchronized boolean[][] column_metadata(MemorySegment stmt) {
        try {
            return $this.column_metadata(stmt);
        } catch (SQLException _) {
            return null;
        }
    }

    @Override
    synchronized void set_commit_listener(boolean enabled) {
        try {
            $this.set_commit_listener(enabled);
        } catch (SQLException _) {
        }
    }

    @Override
    synchronized void set_update_listener(boolean enabled) {
        try {
            $this.set_update_listener(enabled);
        } catch (SQLException _) {
        }
    }

    Arena progressHandlerArena = null;

    public synchronized void register_progress_handler(int vmCalls, ProgressHandler progressHandler)
            throws SQLException {
        $this.register_progress_handler(vmCalls, progressHandler);
    }

    public synchronized void clear_progress_handler() throws SQLException {
        $this.clear_progress_handler();
    }

    /**
     * Getter for native pointer to validate memory is properly cleaned up in unit tests
     *
     * @return a native pointer to validate memory is properly cleaned up in unit tests
     */
    long getBusyHandler() {
        return busyHandlerArena == null ? 0 : 1;
    }

    /**
     * Getter for native pointer to validate memory is properly cleaned up in unit tests
     *
     * @return 0
     * @deprecated This is not relevant with the FFM rewrite. One upcall stub is allocated per DB
     *     object for each of {@code commit_hook()} and {@code rollback_hook()} using {@code
     *     Arena.ofAuto()}.
     */
    @Deprecated
    long getCommitListener() {
        return 0;
    }

    /**
     * Getter for native pointer to validate memory is properly cleaned up in unit tests
     *
     * @return 0
     * @deprecated This is not relevant with the FFM rewrite. One upcall stub is allocated per DB
     *     object for {@code update_hook()} using {@code Arena.ofAuto()}.
     */
    @Deprecated
    long getUpdateListener() {
        return 0;
    }

    /**
     * Getter for native pointer to validate memory is properly cleaned up in unit tests
     *
     * @return a native pointer to validate memory is properly cleaned up in unit tests
     */
    long getProgressHandler() {
        return progressHandlerArena == null ? 0 : 1;
    }

    @Override
    public synchronized byte[] serialize(String schema) throws SQLException {
        return $this.serialize(schema);
    }

    @Override
    public synchronized void deserialize(String schema, byte[] buff) throws SQLException {
        $this.deserialize(schema, buff);
    }
}
