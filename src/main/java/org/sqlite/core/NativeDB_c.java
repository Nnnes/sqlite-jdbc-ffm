package org.sqlite.core;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.sqlite.core.sqlite_h.*;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import org.sqlite.BusyHandler;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.ProgressHandler;
import org.sqlite.core.DB.ProgressObserver;

/**
 * Implements the functionality of the {@code NativeDB.c} of the original JNI version of {@code
 * sqlite-jdbc} as closely as is reasonable using the Java 22+ Foreign Function & Memory API.
 *
 * <p>The most significant difference is that {@link NativeDB_c} keeps all pointers inside {@link
 * MemorySegment}s to avoid the need to pass raw memory addresses around. This changes many of
 * {@link SafeStmtPtr}'s public interfaces in particular.
 */
class NativeDB_c implements Codes {
    private static final MemorySegment SQLITE_TRANSIENT =
            MemorySegment.ofAddress(sqlite_h.SQLITE_TRANSIENT);

    private final NativeDB $this;
    private final MemorySegment commit_hook;
    private final MemorySegment rollback_hook;
    private final MemorySegment update_hook;

    /** The pointer to the DB. Always {@link #ensureOpen()} before passing to a native function. */
    private MemorySegment db = null;

    NativeDB_c(NativeDB $this) {
        this.$this = $this;

        Arena arena = Arena.ofAuto();
        commit_hook commitHookUpcall =
                _ -> {
                    $this.onCommit(true);
                    return 0;
                };
        rollback_hook rollbackHookUpcall = _ -> $this.onCommit(false);
        update_hook updateHookUpcall =
                (_, type, database, table, row) -> {
                    try (Arena stringArena = Arena.ofConfined()) {
                        String databaseString = getString(database, stringArena);
                        String tableString = getString(table, stringArena);

                        $this.onUpdate(type, databaseString, tableString, row);
                    }
                };
        commit_hook = getUpcallStub(commitHookUpcall, arena);
        rollback_hook = getUpcallStub(rollbackHookUpcall, arena);
        update_hook = getUpcallStub(updateHookUpcall, arena);
    }

    private static MemorySegment getUpcallStub(UpcallMethod method, Arena arena) {
        try {
            FunctionDescriptor descriptor = method.descriptor();
            MethodHandle handle =
                    MethodHandles.lookup()
                            .findVirtual(method.getClass(), "call", descriptor.toMethodType());
            return Linker.nativeLinker().upcallStub(handle.bindTo(method), descriptor, arena);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads a UTF-8 string from a zero-length memory segment using a temporary arena.
     *
     * <p>If there is already an available Arena, {@link #getString(MemorySegment, Arena)} should be
     * used instead.
     *
     * @param segment a MemorySegment pointer to a null-terminated UTF-8 string
     * @return a Java string, or null if {@code segment.address()} is a null address
     * @see MemorySegment#getString(long)
     */
    private static String getString(MemorySegment segment) {
        if (hasNullAddress(segment)) return null;
        try (Arena arena = Arena.ofConfined()) {
            return getString(segment, arena);
        }
    }

    /**
     * Reads a UTF-8 string from a zero-length memory segment using the provided arena.
     *
     * <p>If there is not already an available temporary Arena, {@link #getString(MemorySegment)}
     * should be used instead.
     *
     * @param segment a MemorySegment pointer to a null-terminated UTF-8 string
     * @param arena the Arena to be associated with the temporary reinterpreted MemorySegment
     * @return a Java string, or null if {@code segment.address()} is a null address
     * @see MemorySegment#getString(long)
     */
    private static String getString(MemorySegment segment, Arena arena) {
        if (hasNullAddress(segment)) return null;
        return segment.reinterpret(Long.MAX_VALUE, arena, null)
                .getString(0, StandardCharsets.UTF_8);
    }

    private static boolean hasNullAddress(MemorySegment segment) {
        return segment.address() == NULL.address();
    }

    private static byte[] getByteArray(MemorySegment segment, int lengthInBytes, Arena arena) {
        return segment.reinterpret(JAVA_BYTE.byteSize() * lengthInBytes, arena, null)
                .toArray(JAVA_BYTE);
    }

    private static MemorySegment allocateUTF8(String sql, Arena arena) {
        return arena.allocateFrom(sql, StandardCharsets.UTF_8);
    }

    void _open(String file, int flags) throws SQLException {
        if (db != null) {
            // FIXME: ???
            sqlite3_close(db);
            throw new SQLException("DB already open");
        }

        if (file == null) return;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment file_bytes = allocateUTF8(file, arena);

            MemorySegment ppDb = arena.allocate(ADDRESS);
            int ret = sqlite3_open_v2(file_bytes, ppDb, flags, NULL);
            // FIXME: global scope
            db = ppDb.get(ADDRESS, 0);
            if (ret != SQLITE_OK) {
                int errCode = sqlite3_extended_errcode(db);
                String errMsg = errmsg();
                sqlite3_close(db);
                throw DB.newSQLException(errCode, errMsg);
            }
        }

        sqlite3_extended_result_codes(db, 1);
    }

    void _close() throws SQLException {
        if (db != null) {
            change_progress_handler(db, null, 0);
            change_busy_handler(db, null);
            clear_commit_listener(db);
            clear_update_listener(db);

            if (sqlite3_close(db) != SQLITE_OK) {
                throw new SQLException(errmsg());
            }
            db = null;
        }
    }

    int _exec(String sql) throws SQLException {
        ensureOpen();
        if (sql == null) return SQLITE_ERROR;

        int status;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sql_bytes = allocateUTF8(sql, arena);
            status = sqlite3_exec(db, sql_bytes, NULL, NULL, NULL);
        }

        if (status != SQLITE_OK) {
            throw DB.newSQLException(status, errmsg());
        }

        return status;
    }

    int shared_cache(boolean enable) {
        return sqlite3_enable_shared_cache(enable ? 1 : 0);
    }

    int enable_load_extension(boolean enable) throws SQLException {
        ensureOpen();
        return sqlite3_enable_load_extension(db, enable ? 1 : 0);
    }

    void interrupt() throws SQLException {
        ensureOpen();
        sqlite3_interrupt(db);
    }

    void busy_timeout(int ms) throws SQLException {
        ensureOpen();
        sqlite3_busy_timeout(db, ms);
    }

    void busy_handler(BusyHandler busyHandler) throws SQLException {
        ensureOpen();
        change_busy_handler(db, busyHandler);
    }

    MemorySegment prepare(String sql) throws SQLException {
        ensureOpen();
        Objects.requireNonNull(sql);

        Arena stmtArena = Arena.ofAuto();
        MemorySegment stmt;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sql_bytes = allocateUTF8(sql, arena);
            int sql_nbytes = Math.toIntExact(sql_bytes.elements(JAVA_BYTE).count());

            MemorySegment ppStmt = stmtArena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            int status = sqlite3_prepare_v2(db, sql_bytes, sql_nbytes, ppStmt, NULL);
            // FIXME: global scope
            stmt = ppStmt.get(ADDRESS, 0);

            if (status != SQLITE_OK) throw DB.newSQLException(status, errmsg());
        }

        return stmt;
    }

    String errmsg() throws SQLException {
        ensureOpen();
        return getString(sqlite3_errmsg(db));
    }

    String libversion() {
        return getString(sqlite3_libversion());
    }

    long changes() throws SQLException {
        ensureOpen();
        return sqlite3_changes64(db);
    }

    long total_changes() throws SQLException {
        ensureOpen();
        return sqlite3_total_changes64(db);
    }

    int finalize(MemorySegment stmt) throws SQLException {
        return sqlite3_finalize(stmt);
    }

    int step(MemorySegment stmt) throws SQLException {
        return sqlite3_step(stmt);
    }

    int reset(MemorySegment stmt) throws SQLException {
        return sqlite3_reset(stmt);
    }

    int clear_bindings(MemorySegment stmt) throws SQLException {
        return sqlite3_clear_bindings(stmt);
    }

    int bind_parameter_count(MemorySegment stmt) throws SQLException {
        return sqlite3_bind_parameter_count(stmt);
    }

    int column_count(MemorySegment stmt) throws SQLException {
        return sqlite3_column_count(stmt);
    }

    int column_type(MemorySegment stmt, int col) throws SQLException {
        return sqlite3_column_type(stmt, col);
    }

    String column_decltype(MemorySegment stmt, int col) throws SQLException {
        return getString(sqlite3_column_decltype(stmt, col));
    }

    String column_table_name(MemorySegment stmt, int col) throws SQLException {
        return getString(sqlite3_column_table_name(stmt, col));
    }

    String column_name(MemorySegment stmt, int col) throws SQLException {
        return getString(sqlite3_column_name(stmt, col));
    }

    String column_text(MemorySegment stmt, int col) throws SQLException {
        ensureOpen();
        MemorySegment bytes = sqlite3_column_text(stmt, col);

        if (hasNullAddress(bytes)) {
            if (sqlite3_errcode(db) == SQLITE_NOMEM) throw new SQLException("Out of memory");
            return null;
        }

        return getString(bytes);
    }

    byte[] column_blob(MemorySegment stmt, int col) throws SQLException {
        ensureOpen();
        MemorySegment blob = sqlite3_column_blob(stmt, col);
        if (hasNullAddress(blob)) {
            if (sqlite3_errcode(db) == SQLITE_NOMEM) throw new SQLException("Out of memory");

            if (sqlite3_column_type(stmt, col) == SQLITE_NULL) {
                return null;
            } else {
                return new byte[0];
            }
        }

        int length = sqlite3_column_bytes(stmt, col);

        try (Arena arena = Arena.ofConfined()) {
            return getByteArray(blob, length, arena);
        }
    }

    double column_double(MemorySegment stmt, int col) throws SQLException {
        return sqlite3_column_double(stmt, col);
    }

    long column_long(MemorySegment stmt, int col) throws SQLException {
        return sqlite3_column_int64(stmt, col);
    }

    int column_int(MemorySegment stmt, int col) throws SQLException {
        return sqlite3_column_int(stmt, col);
    }

    int bind_null(MemorySegment stmt, int pos) throws SQLException {
        return sqlite3_bind_null(stmt, pos);
    }

    int bind_int(MemorySegment stmt, int pos, int v) throws SQLException {
        return sqlite3_bind_int(stmt, pos, v);
    }

    int bind_long(MemorySegment stmt, int pos, long v) throws SQLException {
        return sqlite3_bind_int64(stmt, pos, v);
    }

    int bind_double(MemorySegment stmt, int pos, double v) throws SQLException {
        return sqlite3_bind_double(stmt, pos, v);
    }

    int bind_text(MemorySegment stmt, int pos, String v) throws SQLException {
        if (v == null) return SQLITE_ERROR;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment v_bytes = allocateUTF8(v, arena);
            int v_nbytes = Math.toIntExact(v_bytes.elements(JAVA_BYTE).count() - 1);

            return sqlite3_bind_text(stmt, pos, v_bytes, v_nbytes, SQLITE_TRANSIENT);
        }
    }

    int bind_blob(MemorySegment stmt, int pos, byte[] v) throws SQLException {
        try (Arena arena = Arena.ofConfined()) {
            return sqlite3_bind_blob(
                    stmt, pos, arena.allocateFrom(JAVA_BYTE, v), v.length, SQLITE_TRANSIENT);
        }
    }

    void result_null(MemorySegment context) {
        if (hasNullAddress(context)) return;
        sqlite3_result_null(context);
    }

    void result_text(MemorySegment context, String value) {
        if (hasNullAddress(context)) return;
        if (value == null) {
            sqlite3_result_null(context);
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value_bytes = allocateUTF8(value, arena);
            int value_nbytes = Math.toIntExact(value_bytes.elements(JAVA_BYTE).count());
            sqlite3_result_text(context, value_bytes, value_nbytes, SQLITE_TRANSIENT);
        }
    }

    void result_blob(MemorySegment context, byte[] value) {
        if (hasNullAddress(context)) return;
        if (value == null) {
            sqlite3_result_null(context);
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            sqlite3_result_blob(
                    context, arena.allocateFrom(JAVA_BYTE, value), value.length, SQLITE_TRANSIENT);
        }
    }

    void result_double(MemorySegment context, double value) {
        if (hasNullAddress(context)) return;
        sqlite3_result_double(context, value);
    }

    void result_long(MemorySegment context, long value) {
        if (hasNullAddress(context)) return;
        sqlite3_result_int64(context, value);
    }

    void result_int(MemorySegment context, int value) {
        if (hasNullAddress(context)) return;
        sqlite3_result_int(context, value);
    }

    void result_error(MemorySegment context, String err) {
        if (hasNullAddress(context)) return;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment err_bytes = allocateUTF8(err, arena);
            int err_nbytes = Math.toIntExact(err_bytes.elements(JAVA_BYTE).count());
            sqlite3_result_error(context, err_bytes, err_nbytes);
        }
    }

    String value_text(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        if (hasNullAddress(value)) return null;

        return getString(sqlite3_value_text(value));
    }

    byte[] value_blob(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        if (hasNullAddress(value)) return null;

        MemorySegment blob = sqlite3_value_blob(value);
        if (hasNullAddress(blob)) return null;

        int length = sqlite3_value_bytes(value);
        try (Arena arena = Arena.ofConfined()) {
            return getByteArray(blob, length, arena);
        }
    }

    double value_double(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return hasNullAddress(value) ? 0 : sqlite3_value_double(value);
    }

    long value_long(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return hasNullAddress(value) ? 0 : sqlite3_value_int64(value);
    }

    int value_int(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return hasNullAddress(value) ? 0 : sqlite3_value_int(value);
    }

    int value_type(Function func, int arg) throws SQLException {
        // FIXME: No NULL guard for the result of tovalue() in NativeDB.c. Likely causes sqlite3 to
        //  crash with an access violation exception if it is NULL.
        return sqlite3_value_type(tovalue(func, arg));
    }

    int create_function(String name, Function func, int nArgs, int flags) throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment name_bytes = allocateUTF8(name, arena);

            int ret;
            if (func instanceof Function.Aggregate) {
                Method xStepMethod = Function.Aggregate.class.getDeclaredMethod("_xStep");
                Method xFinalMethod = Function.Aggregate.class.getDeclaredMethod("_xFinal");
                xStep xStepUpcall =
                        (context, args, value) -> xCall(context, args, value, func, xStepMethod);
                xFinal xFinalUpcall = (context) -> xCall(context, 0, NULL, func, xFinalMethod);
                MemorySegment xStep = getUpcallStub(xStepUpcall, func._arena);
                MemorySegment xFinal = getUpcallStub(xFinalUpcall, func._arena);
                MemorySegment xValue = NULL;
                MemorySegment xInverse = NULL;
                if (func instanceof Function.Window) {
                    Method xValueMethod = Function.Window.class.getDeclaredMethod("_xValue");
                    Method xInverseMethod = Function.Window.class.getDeclaredMethod("_xInverse");
                    xValue xValueUpcall = (context) -> xCall(context, 0, NULL, func, xValueMethod);
                    xInverse xInverseUpcall =
                            (context, args, value) ->
                                    xCall(context, args, value, func, xInverseMethod);
                    xValue = getUpcallStub(xValueUpcall, func._arena);
                    xInverse = getUpcallStub(xInverseUpcall, func._arena);
                }

                ret =
                        sqlite3_create_window_function(
                                db,
                                name_bytes,
                                nArgs,
                                SQLITE_UTF16 | flags,
                                NULL,
                                xStep,
                                xFinal,
                                xValue,
                                xInverse,
                                NULL);
            } else {
                Method xFuncMethod = Function.class.getDeclaredMethod("_xFunc");
                // In NativeDB.c, func is null and the function pointer is passed via context
                xFunc xFuncUpcall =
                        (context, args, value) -> xCall(context, args, value, func, xFuncMethod);
                MemorySegment xFunc = getUpcallStub(xFuncUpcall, func._arena);

                ret =
                        sqlite3_create_function(
                                db,
                                name_bytes,
                                nArgs,
                                SQLITE_UTF16 | flags,
                                NULL,
                                xFunc,
                                NULL,
                                NULL);
            }
            return ret;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    int destroy_function(String name) throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment name_bytes = allocateUTF8(name, arena);

            return sqlite3_create_function(
                    db, name_bytes, -1, SQLITE_UTF16, NULL, NULL, NULL, NULL);
        }
    }

    int create_collation(String name, Collation func) throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            xCompare xCompareUpcall =
                    (_, len1, str1, len2, str2) -> {
                        try (Arena stringArena = Arena.ofConfined()) {
                            byte[] jbyte1 = getByteArray(str1, len1, stringArena);
                            byte[] jbyte2 = getByteArray(str2, len2, stringArena);
                            String jstr1 = new String(jbyte1, StandardCharsets.UTF_8);
                            String jstr2 = new String(jbyte2, StandardCharsets.UTF_8);

                            return func._xCompare(jstr1, jstr2);
                        }
                    };
            MemorySegment xCompare = getUpcallStub(xCompareUpcall, func._arena);

            return sqlite3_create_collation_v2(
                    db,
                    allocateUTF8(name, arena),
                    SQLITE_UTF8, // NativeDB.c uses SQLITE_UTF16
                    NULL,
                    xCompare,
                    NULL);
        }
    }

    int destroy_collation(String name) throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            return sqlite3_create_collation(
                    db, allocateUTF8(name, arena), SQLITE_UTF16, NULL, NULL);
        }
    }

    int limit(int id, int value) throws SQLException {
        ensureOpen();
        return sqlite3_limit(db, id, value);
    }

    int backup(
            String zDBName,
            String zFilename,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeoutLimit,
            int pagesPerStep)
            throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dDBName = allocateUTF8(zDBName, arena);
            MemorySegment dFileName = allocateUTF8(zFilename, arena);

            int flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
            if (sqlite3_strnicmp(dFileName, allocateUTF8("file:", arena), 5) == 0) {
                flags |= SQLITE_OPEN_URI;
            }
            MemorySegment ppFile = arena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            int rc = sqlite3_open_v2(dFileName, ppFile, flags, NULL);

            if (rc == SQLITE_OK) {
                MemorySegment pFile = ppFile.get(ADDRESS, 0);
                MemorySegment pBackup =
                        sqlite3_backup_init(pFile, allocateUTF8("main", arena), db, dDBName);
                if (pBackup.address() != NULL.address()) {
                    copyLoop(pBackup, observer, pagesPerStep, nTimeoutLimit, sleepTimeMillis);

                    sqlite3_backup_finish(pBackup);
                }
                rc = sqlite3_errcode(pFile);
                sqlite3_close(pFile);
            }

            return rc;
        }
    }

    int restore(
            String zDBName,
            String zFilename,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeoutLimit,
            int pagesPerStep)
            throws SQLException {
        ensureOpen();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dDBName = allocateUTF8(zDBName, arena);
            MemorySegment dFileName = allocateUTF8(zFilename, arena);

            int flags = SQLITE_OPEN_READONLY;
            if (sqlite3_strnicmp(dFileName, allocateUTF8("file:", arena), 5) == 0) {
                flags |= SQLITE_OPEN_URI;
            }
            MemorySegment ppFile = arena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            int rc = sqlite3_open_v2(dFileName, ppFile, flags, NULL);

            if (rc == SQLITE_OK) {
                MemorySegment pFile = ppFile.get(ADDRESS, 0);
                MemorySegment pBackup =
                        sqlite3_backup_init(db, dDBName, pFile, allocateUTF8("main", arena));
                if (!hasNullAddress(pBackup)) {
                    copyLoop(pBackup, observer, pagesPerStep, nTimeoutLimit, sleepTimeMillis);
                    sqlite3_backup_finish(pBackup);
                }
                rc = sqlite3_errcode(pFile);
                sqlite3_close(pFile);
            }

            return rc;
        }
    }

    boolean[][] column_metadata(MemorySegment stmt) throws SQLException {
        ensureOpen();
        int colCount = sqlite3_column_count(stmt);
        boolean[][] array = new boolean[colCount][3];

        for (int i = 0; i < colCount; i++) {
            MemorySegment zColumnName = sqlite3_column_name(stmt, i);
            MemorySegment zTableName = sqlite3_column_table_name(stmt, i);

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pNotNull = arena.allocate(JAVA_INT);
                pNotNull.set(JAVA_INT, 0, 0);
                MemorySegment pPrimaryKey = arena.allocate(JAVA_INT);
                pPrimaryKey.set(JAVA_INT, 0, 0);
                MemorySegment pAutoinc = arena.allocate(JAVA_INT);
                pAutoinc.set(JAVA_INT, 0, 0);

                if (!hasNullAddress(zTableName) && !hasNullAddress(zColumnName)) {
                    sqlite3_table_column_metadata(
                            db,
                            NULL,
                            zTableName,
                            zColumnName,
                            NULL,
                            NULL,
                            pNotNull,
                            pPrimaryKey,
                            pAutoinc);
                }

                array[i][0] = pNotNull.get(JAVA_INT, 0) != 0;
                array[i][1] = pPrimaryKey.get(JAVA_INT, 0) != 0;
                array[i][2] = pAutoinc.get(JAVA_INT, 0) != 0;
            }
        }

        return array;
    }

    void set_commit_listener(boolean enabled) throws SQLException {
        ensureOpen();
        if (enabled) {
            sqlite3_commit_hook(db, commit_hook, NULL);
            sqlite3_rollback_hook(db, rollback_hook, NULL);
        } else {
            clear_commit_listener(db);
        }
    }

    void set_update_listener(boolean enabled) throws SQLException {
        ensureOpen();
        if (enabled) {
            sqlite3_update_hook(db, update_hook, NULL);
        } else {
            clear_update_listener(db);
        }
    }

    void register_progress_handler(int vmCalls, ProgressHandler progressHandler)
            throws SQLException {
        ensureOpen();
        change_progress_handler(db, progressHandler, vmCalls);
    }

    void clear_progress_handler() throws SQLException {
        ensureOpen();
        change_progress_handler(db, null, 0);
    }

    byte[] serialize(String jschema) throws SQLException {
        ensureOpen();

        try (Arena arena = Arena.ofConfined()) {
            // FIXME: may fail if jschema contains null characters! JNI GetStringUTFChars
            MemorySegment schema = allocateUTF8(jschema, arena);

            MemorySegment pSize = arena.allocate(ADDRESS.withTargetLayout(JAVA_LONG));
            boolean need_free = false;
            MemorySegment buff = sqlite3_serialize(db, schema, pSize, SQLITE_SERIALIZE_NOCOPY);
            if (hasNullAddress(buff)) {
                buff = sqlite3_serialize(db, schema, pSize, 0);
                if (hasNullAddress(buff)) {
                    throw new SQLException("Serialization failed, allocation failed");
                }
                need_free = true;
            }

            long size = pSize.get(JAVA_LONG, 0);
            byte[] jbuff = getByteArray(buff, Math.toIntExact(size), arena);

            if (need_free) {
                sqlite3_free(buff);
            }

            return jbuff;
        }
    }

    void deserialize(String jschema, byte[] jbuff) throws SQLException {
        ensureOpen();

        int size = jbuff.length;
        MemorySegment sqlite_buff = sqlite3_malloc(size);
        if (hasNullAddress(sqlite_buff)) {
            throw new SQLException("Failed to allocate native memory for database");
        }

        try (Arena arena = Arena.ofConfined()) {
            // TODO: figure out if this can be simplified
            // doesn't work to just remove SQLITE_DESERIALIZE_FREEONCLOSE and use buff
            MemorySegment buff = arena.allocateFrom(JAVA_BYTE, jbuff);
            MemorySegment pSqliteBuff = sqlite_buff.reinterpret(size, arena, null);
            pSqliteBuff.copyFrom(buff);

            // FIXME: may fail if jschema contains null characters! JNI GetStringUTFChars
            MemorySegment schema = allocateUTF8(jschema, arena);
            int ret =
                    sqlite3_deserialize(
                            db,
                            schema,
                            pSqliteBuff,
                            size,
                            size,
                            SQLITE_DESERIALIZE_FREEONCLOSE | SQLITE_DESERIALIZE_RESIZEABLE);
            if (ret == SQLITE_OK) {
                MemorySegment max_size = arena.allocate(ADDRESS.withTargetLayout(JAVA_LONG));
                max_size.set(JAVA_LONG, 0, 1024L * 1024L * 1000L * 2L);
                sqlite3_file_control(db, schema, SQLITE_FCNTL_SIZE_LIMIT, max_size);
            } else {
                throw DB.newSQLException(ret, errmsg());
            }
        }
    }

    /**
     * Does nothing if {@link #db} is open.
     *
     * @throws SQLException if {@link #db} is closed
     */
    private void ensureOpen() throws SQLException {
        if (db == null) throw new SQLException("The database has been closed");
    }

    // TODO: accept Arena argument
    private MemorySegment tovalue(Function function, int arg) throws SQLException {
        if (arg < 0) throw new SQLException("negative arg out of range");
        if (function == null) throw new SQLException("inconsistent function");

        MemorySegment value_pntr = function._getValue();
        int numArgs = function._getArgs();

        if (hasNullAddress(value_pntr)) throw new SQLException("no current value");
        if (arg >= numArgs) throw new SQLException("arg out of range");

        try (Arena arena = Arena.ofConfined()) {
            return value_pntr
                    .reinterpret(ADDRESS.byteSize() * numArgs, arena, null)
                    // FIXME: global arena
                    .getAtIndex(ADDRESS, arg);
        }
    }

    private void change_progress_handler(
            MemorySegment db, ProgressHandler progressHandler, int vmCalls) {
        if ($this.progressHandlerArena != null) $this.progressHandlerArena.close();
        if (progressHandler != null) {
            $this.progressHandlerArena = Arena.ofShared();
            progress_handler_function progressHandlerUpcall = _ -> progressHandler._progress();
            MemorySegment progress_handler_function =
                    getUpcallStub(progressHandlerUpcall, $this.progressHandlerArena);
            sqlite3_progress_handler(db, vmCalls, progress_handler_function, NULL);
        } else {
            $this.progressHandlerArena = null;
            sqlite3_progress_handler(db, 0, NULL, NULL);
        }
    }

    private void change_busy_handler(MemorySegment db, BusyHandler busyHandler) {
        if ($this.busyHandlerArena != null) $this.busyHandlerArena.close();
        if (busyHandler != null) {
            $this.busyHandlerArena = Arena.ofShared();
            busyHandlerCallBack busyHandlerUpcall =
                    (_, nbPrevInvok) -> busyHandler._callback(nbPrevInvok);
            MemorySegment busyHandlerCallBack =
                    getUpcallStub(busyHandlerUpcall, $this.busyHandlerArena);
            sqlite3_busy_handler(db, busyHandlerCallBack, NULL);
        } else {
            $this.busyHandlerArena = null;
            sqlite3_busy_handler(db, NULL, NULL);
        }
    }

    private void clear_commit_listener(MemorySegment db) {
        sqlite3_commit_hook(db, NULL, NULL);
        sqlite3_rollback_hook(db, NULL, NULL);
    }

    private void clear_update_listener(MemorySegment db) {
        sqlite3_update_hook(db, NULL, NULL);
    }

    // TODO: rewrite with less reflection (maybe remove xCall())
    private void xCall(
            MemorySegment context, int args, MemorySegment value, Function func, Method method) {
        try (Arena arena = Arena.ofConfined()) {
            func._setContext(context);
            func._setValue(value.reinterpret(ADDRESS.byteSize(), arena, null));
            func._setArgs(args);

            try {
                method.invoke(func);
            } catch (Throwable e) {
                xFunc_error(context, e);
            }

            func._setContext(NULL);
            func._setValue(NULL);
            func._setArgs(0);
        }
    }

    private void xFunc_error(MemorySegment context, Throwable ex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment msg_bytes = allocateUTF8(ex.toString(), arena);
            int msg_nbytes = Math.toIntExact(msg_bytes.elements(JAVA_BYTE).count());

            sqlite3_result_error(context, msg_bytes, msg_nbytes);
        }
    }

    private void copyLoop(
            MemorySegment pBackup,
            ProgressObserver progress,
            int pagesPerStep,
            int nTimeoutLimit,
            int sleepTimeMillis) {
        int rc;
        int nTimeout = 0;

        do {
            rc = sqlite3_backup_step(pBackup, pagesPerStep);

            if (rc == SQLITE_OK || rc == SQLITE_DONE) {
                if (progress == null) continue;
                int remaining = sqlite3_backup_remaining(pBackup);
                int pagecount = sqlite3_backup_pagecount(pBackup);
                progress.progress(remaining, pagecount);
            } else if (rc == SQLITE_BUSY || rc == SQLITE_LOCKED) {
                if (nTimeout++ >= nTimeoutLimit) break;
                sqlite3_sleep(sleepTimeMillis);
            }
        } while (rc == SQLITE_OK || rc == SQLITE_BUSY || rc == SQLITE_LOCKED);
    }

    private interface UpcallMethod {
        FunctionDescriptor descriptor();
    }

    @FunctionalInterface
    private interface xFunc extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xStep extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xInverse extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xValue extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() uses via reflection
        void call(MemorySegment context) throws SQLException;
    }

    @FunctionalInterface
    private interface xFinal extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        void call(MemorySegment context) throws SQLException;
    }

    @FunctionalInterface
    private interface xCompare extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        int call(MemorySegment context, int len1, MemorySegment str1, int len2, MemorySegment str2);
    }

    @FunctionalInterface
    private interface commit_hook extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        int call(MemorySegment pArg);
    }

    @FunctionalInterface
    private interface rollback_hook extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        void call(MemorySegment pArg);
    }

    @FunctionalInterface
    private interface update_hook extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        void call(
                MemorySegment context,
                int type,
                MemorySegment database,
                MemorySegment table,
                long row);
    }

    @FunctionalInterface
    private interface progress_handler_function extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        int call(MemorySegment ctx) throws SQLException;
    }

    @FunctionalInterface
    private interface busyHandlerCallBack extends UpcallMethod {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
        }

        @SuppressWarnings("unused")
        // getUpcallStub() calls via reflection
        int call(MemorySegment callback, int nbPrevInvok) throws SQLException;
    }
}
