package org.sqlite.core;

import org.sqlite.BusyHandler;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.ProgressHandler;
import org.sqlite.core.DB.ProgressObserver;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.sqlite.core.sqlite_h.*;

/// Implements the functionality of the `NativeDB.c` of the original JNI version of `sqlite-jdbc` as closely as is
/// reasonable using the Java 22+ Foreign Function & Memory API.
///
/// The most significant difference is that `NativeDB_c` keeps all pointers inside [MemorySegment]s to avoid the need
/// to pass raw memory addresses around. This changes many of [SafeStmtPtr]'s public interfaces in particular.
class NativeDB_c implements Codes {
    private final NativeDB $this;
    private final MemorySegment commit_hook;
    private final MemorySegment rollback_hook;
    private final MemorySegment update_hook;
    MemorySegment dbpointer;

    NativeDB_c(NativeDB $this) {
        this.$this = $this;

        Arena arena = Arena.ofAuto();
        commit_hook = getUpcallStub((commit_hook) _ -> {
            $this.onCommit(true);
            return 0;
        }, arena);
        rollback_hook = getUpcallStub((rollback_hook) _ -> $this.onCommit(false), arena);
        update_hook = getUpcallStub((update_hook) (_, type, database, table, row) -> {
            try (Arena stringArena = Arena.ofConfined()) {
                MemorySegment pDatabase = database.reinterpret(Long.MAX_VALUE, stringArena, null);
                MemorySegment pTable = table.reinterpret(Long.MAX_VALUE, stringArena, null);
                String databaseString = pDatabase.getString(0, StandardCharsets.UTF_8);
                String tableString = pTable.getString(0, StandardCharsets.UTF_8);

                $this.onUpdate(type, databaseString, tableString, row);
            }
        }, arena);
    }

    private static MemorySegment getUpcallStub(UpcallStub function, Arena arena) {
        try {
            return Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findVirtual(
                            function.getClass(), "call", function.descriptor().toMethodType()
                    ).bindTo(function), function.descriptor(), arena);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    void _open(String file, int flags) throws SQLException {
        MemorySegment db;
        int ret;

        db = gethandle();
        if (db != null) {
            throwex_msg("DB already open");
            sqlite3_close(db);
            return;
        }

        if (file == null) return;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment file_bytes = arena.allocateFrom(file, StandardCharsets.UTF_8);

            MemorySegment ppDb = arena.allocate(ADDRESS);
            ret = sqlite3_open_v2(file_bytes, ppDb, flags, NULL);
            db = ppDb.get(ADDRESS, 0);
        }

        sethandle(db);
        if (ret != SQLITE_OK) {
            ret = sqlite3_extended_errcode(db);
            throwex_errorcode(ret);
            sethandle(null);
            sqlite3_close(db);
            return;
        }

        sqlite3_extended_result_codes(dbpointer, 1);
    }

    void _close() throws SQLException {
        MemorySegment db = gethandle();
        if (db != null) {
            change_progress_handler(null, 0);
            change_busy_handler(null);
            clear_commit_listener(db);
            clear_update_listener();

            if (sqlite3_close(db) != SQLITE_OK) {
                throwex();
            }
            sethandle(null);
        }
    }

    int _exec(String sql) throws SQLException {
        MemorySegment db;
        MemorySegment sql_bytes;
        int status;

        db = gethandle();
        if (db == null) {
            throwex_errorcode(SQLITE_MISUSE);
            return SQLITE_MISUSE;
        }

        if (sql == null) return SQLITE_ERROR;

        try (Arena arena = Arena.ofConfined()) {
            sql_bytes = arena.allocateFrom(sql, StandardCharsets.UTF_8);

            status = sqlite3_exec(db, sql_bytes, NULL, NULL, NULL);
        }

        if (status != SQLITE_OK) {
            throwex_errorcode(status);
        }

        return status;
    }

    int shared_cache(boolean enable) {
        return sqlite3_enable_shared_cache(enable ? 1 : 0);
    }

    int enable_load_extension(boolean enable) throws SQLException {
        MemorySegment db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return SQLITE_MISUSE;
        }

        return sqlite3_enable_load_extension(db, enable ? 1 : 0);
    }

    void interrupt() throws SQLException {
        MemorySegment db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return;
        }

        sqlite3_interrupt(db);
    }

    void busy_timeout(int ms) throws SQLException {
        MemorySegment db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return;
        }

        sqlite3_busy_timeout(db, ms);
    }

    void busy_handler(BusyHandler busyHandler) throws SQLException {
        change_busy_handler(busyHandler);
    }

    MemorySegment prepare(String sql) throws SQLException {
        MemorySegment db;
        MemorySegment stmt;
        MemorySegment sql_bytes;
        int sql_nbytes;
        int status;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return null;
        }

        if (sql == null) return null;

        Arena stmtArena = Arena.ofAuto();
        try (Arena arena = Arena.ofConfined()) {
            sql_bytes = arena.allocateFrom(sql, StandardCharsets.UTF_8);
            sql_nbytes = Math.toIntExact(sql_bytes.elements(JAVA_BYTE).count());

            MemorySegment ppStmt = stmtArena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            status = sqlite3_prepare_v2(db, sql_bytes, sql_nbytes, ppStmt, NULL);
            stmt = ppStmt.get(ADDRESS, 0);

            if (status != SQLITE_OK) {
                throwex_errorcode(status);
                return null;
            }
        }

        return stmt;
    }

    String errmsg() throws SQLException {
        MemorySegment db;
        MemorySegment str;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return null;
        }

        str = sqlite3_errmsg(db);
        if (str.address() == NULL.address()) return null;
        try (Arena arena = Arena.ofConfined()) {
            return str.reinterpret(Long.MAX_VALUE, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    String libversion() {
        MemorySegment version = sqlite3_libversion();
        try (Arena arena = Arena.ofConfined()) {
            return version.reinterpret(Long.MAX_VALUE, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    long changes() throws SQLException {
        MemorySegment db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return 0;
        }

        return sqlite3_changes64(db);
    }

    long total_changes() throws SQLException {
        MemorySegment db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return 0;
        }

        return sqlite3_total_changes64(db);
    }

    int finalize(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_finalize(stmt);
    }

    int step(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_step(stmt);
    }

    int reset(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_reset(stmt);
    }

    int clear_bindings(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_clear_bindings(stmt);
    }

    int bind_parameter_count(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_bind_parameter_count(stmt);
    }

    int column_count(MemorySegment stmt) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_column_count(stmt);
    }

    int column_type(MemorySegment stmt, int col) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_column_type(stmt, col);
    }

    String column_decltype(MemorySegment stmt, int col) throws SQLException {
        MemorySegment str;

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        str = sqlite3_column_decltype(stmt, col);
        if (str.address() == NULL.address()) return null;
        try (Arena arena = Arena.ofConfined()) {
            return str.reinterpret(Long.MAX_VALUE, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    String column_table_name(MemorySegment stmt, int col) throws SQLException {
        MemorySegment str;

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        str = sqlite3_column_table_name(stmt, col);
        if (str.address() == NULL.address()) return null;
        try (Arena arena = Arena.ofConfined()) {
            return str.reinterpret(Long.MAX_VALUE, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    String column_name(MemorySegment stmt, int col) throws SQLException {
        MemorySegment str;

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        str = sqlite3_column_name(stmt, col);
        if (str.address() == NULL.address()) return null;

        try (Arena arena = Arena.ofConfined()) {
            return str.reinterpret(Long.MAX_VALUE, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    String column_text(MemorySegment stmt, int col) throws SQLException {
        MemorySegment db;
        MemorySegment bytes;
        int nbytes;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return null;
        }

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        bytes = sqlite3_column_text(stmt, col);
        nbytes = sqlite3_column_bytes(stmt, col);

        if (bytes.address() == NULL.address()) {
            if (sqlite3_errcode(db) == SQLITE_NOMEM) {
                throwex_outofmemory();
            }
            return null;
        }

        try (Arena arena = Arena.ofConfined()) {
            return bytes.reinterpret(nbytes + 1, arena, null).getString(0, StandardCharsets.UTF_8);
        }
    }

    byte[] column_blob(MemorySegment stmt, int col) throws SQLException {
        MemorySegment db;
        int type;
        int length;
        byte[] jBlob;
        MemorySegment blob;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return null;
        }

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        type = sqlite3_column_type(stmt, col);
        blob = sqlite3_column_blob(stmt, col);
        if (blob.address() == NULL.address() && sqlite3_errcode(db) == SQLITE_NOMEM) {
            throwex_outofmemory();
            return null;
        }
        if (blob.address() == NULL.address()) {
            if (type == SQLITE_NULL) {
                return null;
            } else {
                jBlob = new byte[0];
                return jBlob;
            }
        }

        length = sqlite3_column_bytes(stmt, col);

        try (Arena arena = Arena.ofConfined()) {
            jBlob = blob.reinterpret(JAVA_BYTE.byteSize() * length, arena, null).toArray(JAVA_BYTE);
        }

        return jBlob;
    }

    double column_double(MemorySegment stmt, int col) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return 0;
        }

        return sqlite3_column_double(stmt, col);
    }

    long column_long(MemorySegment stmt, int col) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return 0;
        }

        return sqlite3_column_int64(stmt, col);
    }

    int column_int(MemorySegment stmt, int col) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return 0;
        }

        return sqlite3_column_int(stmt, col);
    }

    int bind_null(MemorySegment stmt, int pos) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_bind_null(stmt, pos);
    }

    int bind_int(MemorySegment stmt, int pos, int v) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_bind_int(stmt, pos, v);
    }

    int bind_long(MemorySegment stmt, int pos, long v) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_bind_int64(stmt, pos, v);
    }

    int bind_double(MemorySegment stmt, int pos, double v) throws SQLException {
        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        return sqlite3_bind_double(stmt, pos, v);
    }

    int bind_text(MemorySegment stmt, int pos, String v) throws SQLException {
        int rc;
        MemorySegment v_bytes;
        int v_nbytes;

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        if (v == null) return SQLITE_ERROR;
        try (Arena arena = Arena.ofConfined()) {
            v_bytes = arena.allocateFrom(v, StandardCharsets.UTF_8);
            v_nbytes = Math.toIntExact(v_bytes.elements(JAVA_BYTE).count() - 1);

            rc = sqlite3_bind_text(stmt, pos, v_bytes, v_nbytes, MemorySegment.ofAddress(SQLITE_TRANSIENT));
        }
        return rc;
    }

    int bind_blob(MemorySegment stmt, int pos, byte[] v) throws SQLException {
        int rc;
        MemorySegment a;
        int size;

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return SQLITE_MISUSE;
        }

        size = v.length;
        try (Arena arena = Arena.ofConfined()) {
            a = arena.allocateFrom(JAVA_BYTE, v);
            rc = sqlite3_bind_blob(stmt, pos, a, size, MemorySegment.ofAddress(SQLITE_TRANSIENT));
        }
        return rc;
    }

    void result_null(MemorySegment context) {
        if (context.address() == NULL.address()) return;
        sqlite3_result_null(context);
    }

    void result_text(MemorySegment context, String value) {
        MemorySegment value_bytes;
        int value_nbytes;

        if (context.address() == NULL.address()) return;
        if (value == null) {
            sqlite3_result_null(context);
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            value_bytes = arena.allocateFrom(value, StandardCharsets.UTF_8);
            value_nbytes = Math.toIntExact(value_bytes.elements(JAVA_BYTE).count());
            sqlite3_result_text(context, value_bytes, value_nbytes, MemorySegment.ofAddress(SQLITE_TRANSIENT));
        }
    }

    void result_blob(MemorySegment context, byte[] value) {
        MemorySegment bytes;
        int size;

        if (context.address() == NULL.address()) return;
        if (value == null) {
            sqlite3_result_null(context);
            return;
        }

        size = value.length;
        try (Arena arena = Arena.ofConfined()) {
            bytes = arena.allocateFrom(JAVA_BYTE, value);
            sqlite3_result_blob(context, bytes, size, MemorySegment.ofAddress(SQLITE_TRANSIENT));
        }
    }

    void result_double(MemorySegment context, double value) {
        if (context.address() == NULL.address()) return;
        sqlite3_result_double(context, value);
    }

    void result_long(MemorySegment context, long value) {
        if (context.address() == NULL.address()) return;
        sqlite3_result_int64(context, value);
    }

    void result_int(MemorySegment context, int value) {
        if (context.address() == NULL.address()) return;
        sqlite3_result_int(context, value);
    }

    void result_error(MemorySegment context, String err) {
        MemorySegment err_bytes;
        int err_nbytes;

        if (context.address() == NULL.address()) return;

        try (Arena arena = Arena.ofConfined()) {
            err_bytes = arena.allocateFrom(err, StandardCharsets.UTF_8);
            err_nbytes = Math.toIntExact(err_bytes.elements(JAVA_BYTE).count());
            sqlite3_result_error(context, err_bytes, err_nbytes);
        }
    }

    String value_text(Function f, int arg) throws SQLException {
        MemorySegment bytes;
        int nbytes;

        MemorySegment value = tovalue(f, arg);
        if (value.address() == NULL.address()) return null;

        bytes = sqlite3_value_text(value);
        nbytes = sqlite3_value_bytes(value);

        try (Arena arena = Arena.ofConfined()) {
            return bytes.reinterpret(JAVA_BYTE.byteSize() * nbytes + 1, arena, null)
                    .getString(0, StandardCharsets.UTF_8);
        }
    }

    byte[] value_blob(Function f, int arg) throws SQLException {
        int length;
        byte[] jBlob;
        MemorySegment blob;
        MemorySegment value = tovalue(f, arg);
        if (value.address() == NULL.address()) return null;

        blob = sqlite3_value_blob(value);
        if (blob.address() == NULL.address()) return null;

        length = sqlite3_value_bytes(value);
        try (Arena arena = Arena.ofConfined()) {
            jBlob = blob.reinterpret(length, arena, null).toArray(JAVA_BYTE);
        }

        return jBlob;
    }

    double value_double(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return (value.address() != NULL.address()) ? sqlite3_value_double(value) : 0;
    }

    long value_long(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return (value.address() != NULL.address()) ? sqlite3_value_int64(value) : 0;
    }

    int value_int(Function f, int arg) throws SQLException {
        MemorySegment value = tovalue(f, arg);
        return (value.address() != NULL.address()) ? sqlite3_value_int(value) : 0;
    }

    int value_type(Function func, int arg) throws SQLException {
        // FIXME: No NULL guard for the result of tovalue() in NativeDB.c. Likely causes sqlite3 to crash with an
        //  access violation exception if if is NULL.
        return sqlite3_value_type(tovalue(func, arg));
    }

    int create_function(String name, Function func, int nArgs, int flags) {
        int ret;
        MemorySegment name_bytes;
        boolean isAgg;
        boolean isWindow;

        isAgg = func instanceof Function.Aggregate;
        isWindow = func instanceof Function.Window;

        try (Arena arena = Arena.ofConfined()) {
            name_bytes = arena.allocateFrom(name, StandardCharsets.UTF_8);

            if (isAgg) {
                Method xStepMethod = Function.Aggregate.class.getDeclaredMethod("_xStep");
                Method xFinalMethod = Function.Aggregate.class.getDeclaredMethod("_xFinal");
                MemorySegment xStep = getUpcallStub(
                        (xStep) (context, args, value) -> xCall(context, args, value, func, xStepMethod), func._arena);
                MemorySegment xFinal = getUpcallStub(
                        (xFinal) (context) -> xCall(context, 0, NULL, func, xFinalMethod), func._arena);
                MemorySegment xValue = NULL;
                MemorySegment xInverse = NULL;
                if (isWindow) {
                    Method xValueMethod = Function.Window.class.getDeclaredMethod("_xValue");
                    Method xInverseMethod = Function.Window.class.getDeclaredMethod("_xInverse");
                    xValue = getUpcallStub(
                            (xValue) (context) -> xCall(context, 0, NULL, func, xValueMethod), func._arena);
                    xInverse = getUpcallStub(
                            (xInverse) (context, args, value) -> xCall(context, args, value, func, xInverseMethod),
                            func._arena);
                }

                ret = sqlite3_create_window_function(
                        gethandle(),
                        name_bytes,
                        nArgs,
                        SQLITE_UTF16 | flags,
                        NULL, // UDFData udf
                        xStep,
                        xFinal,
                        xValue,
                        xInverse,
                        NULL
                );
            } else {
                Method xFuncMethod = Function.class.getDeclaredMethod("_xFunc");
                // In NativeDB.c, func is null and the function pointer is passed via context
                MemorySegment xFunc = getUpcallStub(
                        (xFunc) (context, args, value) -> xCall(context, args, value, func, xFuncMethod), func._arena);

                ret = sqlite3_create_function_v2(
                        gethandle(),
                        name_bytes,
                        nArgs,
                        SQLITE_UTF16 | flags,
                        NULL, // UDFData udf
                        xFunc,
                        NULL,
                        NULL,
                        NULL
                );
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        return ret;
    }

    int destroy_function(String name) {
        int ret;
        MemorySegment name_bytes;

        try (Arena arena = Arena.ofConfined()) {
            name_bytes = arena.allocateFrom(name);

            ret = sqlite3_create_function(
                    gethandle(), name_bytes, -1, SQLITE_UTF16, NULL, NULL, NULL, NULL
            );
        }

        return ret;
    }

    int create_collation(String name, Collation func) {
        int ret;
        MemorySegment name_bytes;

        try (Arena arena = Arena.ofConfined()) {
            name_bytes = arena.allocateFrom(name, StandardCharsets.UTF_8);

            MemorySegment xCompare = getUpcallStub((xCompare) (_, len1, str1, len2, str2) -> {
                try (Arena stringArena = Arena.ofConfined()) {
                    String jstr1 = new String(str1.reinterpret(len1, stringArena, null).toArray(JAVA_BYTE),
                            StandardCharsets.UTF_8);
                    String jstr2 = new String(str2.reinterpret(len2, stringArena, null).toArray(JAVA_BYTE),
                            StandardCharsets.UTF_8);

                    return func._xCompare(jstr1, jstr2);
                }
            }, func._arena);

            ret = sqlite3_create_collation_v2(
                    gethandle(),
                    name_bytes,
                    SQLITE_UTF8, // NativeDB.c uses SQLITE_UTF16
                    NULL, // CollationData coll
                    xCompare,
                    NULL
            );
        }

        return ret;
    }

    int destroy_collation(String name) {
        int ret;
        MemorySegment name_bytes;

        try (Arena arena = Arena.ofConfined()) {
            name_bytes = arena.allocateFrom(name, StandardCharsets.UTF_8);

            ret = sqlite3_create_collation(
                    gethandle(), name_bytes, SQLITE_UTF16, NULL, NULL
            );
        }

        return ret;
    }

    int limit(int id, int value) throws SQLException {
        MemorySegment db;

        db = gethandle();

        if (db.address() == NULL.address()) {
            throwex_db_closed();
            return 0;
        }

        return sqlite3_limit(db, id, value);
    }

    int backup(
            String zDBName,
            String zFilename,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeoutLimit,
            int pagesPerStep
    ) throws SQLException {
        if (sqlite3_libversion_number() < 3006011) return SQLITE_INTERNAL;
        int rc;
        MemorySegment pDb;
        MemorySegment pFile = NULL;
        MemorySegment pBackup;
        MemorySegment dFileName;
        MemorySegment dDBName;

        pDb = gethandle();
        if (pDb.address() == NULL.address()) {
            throwex_db_closed();
            return SQLITE_MISUSE;
        }

        try (Arena arena = Arena.ofConfined()) {
            dFileName = arena.allocateFrom(zFilename, StandardCharsets.UTF_8);

            dDBName = arena.allocateFrom(zDBName, StandardCharsets.UTF_8);

            int flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
            if (sqlite3_strnicmp(dFileName, arena.allocateFrom("file:", StandardCharsets.UTF_8), 5) == 0) {
                flags |= SQLITE_OPEN_URI;
            }
            MemorySegment ppFile = arena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            rc = sqlite3_open_v2(dFileName, ppFile, flags, NULL);

            if (rc == SQLITE_OK) {
                pFile = ppFile.get(ADDRESS, 0);
                pBackup = sqlite3_backup_init(
                        pFile, arena.allocateFrom("main", StandardCharsets.UTF_8), pDb, dDBName);
                if (pBackup.address() != NULL.address()) {
                    copyLoop(pBackup, observer, pagesPerStep, nTimeoutLimit, sleepTimeMillis);

                    sqlite3_backup_finish(pBackup);
                }
                rc = sqlite3_errcode(pFile);
            }

            sqlite3_close(pFile);
        }

        return rc;
    }

    int restore(
            String zDBName,
            String zFilename,
            ProgressObserver observer,
            int sleepTimeMillis,
            int nTimeoutLimit,
            int pagesPerStep
    ) throws SQLException {
        if (sqlite3_libversion_number() < 3006011) return SQLITE_INTERNAL;
        int rc;
        MemorySegment pDb;
        MemorySegment pFile = NULL;
        MemorySegment pBackup;
        MemorySegment dFileName;
        MemorySegment dDBName;

        pDb = gethandle();
        if (pDb.address() == NULL.address()) {
            throwex_db_closed();
            return SQLITE_MISUSE;
        }

        try (Arena arena = Arena.ofConfined()) {
            dFileName = arena.allocateFrom(zFilename, StandardCharsets.UTF_8);

            dDBName = arena.allocateFrom(zDBName, StandardCharsets.UTF_8);

            int flags = SQLITE_OPEN_READONLY;
            if (sqlite3_strnicmp(dFileName, arena.allocateFrom("file:", StandardCharsets.UTF_8), 5) == 0) {
                flags |= SQLITE_OPEN_URI;
            }
            MemorySegment ppFile = arena.allocate(ADDRESS.withTargetLayout(ADDRESS));
            rc = sqlite3_open_v2(dFileName, ppFile, flags, NULL);

            if (rc == SQLITE_OK) {
                pFile = ppFile.get(ADDRESS, 0);
                pBackup = sqlite3_backup_init(
                        pDb, dDBName, pFile, arena.allocateFrom("main", StandardCharsets.UTF_8));
                if (pBackup.address() != NULL.address()) {
                    copyLoop(pBackup, observer, pagesPerStep, nTimeoutLimit, sleepTimeMillis);
                    sqlite3_backup_finish(pBackup);
                }
                rc = sqlite3_errcode(pFile);
            }

            sqlite3_close(pFile);
        }

        return rc;
    }

    boolean[][] column_metadata(MemorySegment stmt) throws SQLException {
        MemorySegment zTableName, zColumnName;
        MemorySegment pNotNull, pPrimaryKey, pAutoinc;
        int i, colCount;
        boolean[][] array;
        boolean[] colData;
        boolean[] colDataRaw; // TODO: rewrite JNI-specific code
        MemorySegment db;
        MemorySegment dbstmt;

        db = gethandle();
        if (db.address() == NULL.address()) {
            throwex_db_closed();
            return null;
        }

        if (stmt.address() == NULL.address()) {
            throwex_stmt_finalized();
            return null;
        }

        dbstmt = stmt;

        colCount = sqlite3_column_count(dbstmt);
        array = new boolean[colCount][];

        colDataRaw = new boolean[3];

        for (i = 0; i < colCount; i++) {
            zColumnName = sqlite3_column_name(dbstmt, i);
            zTableName = sqlite3_column_table_name(dbstmt, i);

            try (Arena arena = Arena.ofConfined()) {
                pNotNull = arena.allocate(JAVA_INT);
                pNotNull.set(JAVA_INT, 0, 0);
                pPrimaryKey = arena.allocate(JAVA_INT);
                pPrimaryKey.set(JAVA_INT, 0, 0);
                pAutoinc = arena.allocate(JAVA_INT);
                pAutoinc.set(JAVA_INT, 0, 0);

                if (zTableName.address() != NULL.address() && zColumnName.address() != NULL.address()) {
                    sqlite3_table_column_metadata(
                            db, NULL, zTableName, zColumnName,
                            NULL, NULL, pNotNull, pPrimaryKey, pAutoinc
                    );
                }

                colDataRaw[0] = pNotNull.get(JAVA_INT, 0) != 0;
                colDataRaw[1] = pPrimaryKey.get(JAVA_INT, 0) != 0;
                colDataRaw[2] = pAutoinc.get(JAVA_INT, 0) != 0;
            }

            colData = new boolean[3];

            System.arraycopy(colDataRaw, 0, colData, 0, 3);
            array[i] = colData;
        }

        return array;
    }

    void set_commit_listener(boolean enabled) {
        MemorySegment db = gethandle();
        if (enabled) {
            sqlite3_commit_hook(db, commit_hook, NULL);
            sqlite3_rollback_hook(db, rollback_hook, NULL);
        } else {
            clear_commit_listener(db);
        }
    }

    void set_update_listener(boolean enabled) {
        if (enabled) {
            sqlite3_update_hook(gethandle(), update_hook, NULL);
        } else {
            clear_update_listener();
        }
    }

    void register_progress_handler(int vmCalls, ProgressHandler progressHandler) throws SQLException {
        change_progress_handler(progressHandler, vmCalls);
    }

    void clear_progress_handler() throws SQLException {
        change_progress_handler(null, 0);
    }

    byte[] serialize(String jschema) throws SQLException {
        MemorySegment db = gethandle();
        if (db.address() == NULL.address()) {
            throwex_db_closed();
            return null;
        }

        try (Arena arena = Arena.ofConfined()) {
            // FIXME: may fail if jschema contains null characters! JNI GetStringUTFChars
            MemorySegment schema = arena.allocateFrom(jschema, StandardCharsets.UTF_8);

            MemorySegment pSize = arena.allocate(ADDRESS.withTargetLayout(JAVA_LONG));
            boolean need_free = false;
            MemorySegment buff = sqlite3_serialize(db, schema, pSize, SQLITE_SERIALIZE_NOCOPY);
            if (buff.address() == NULL.address()) {
                buff = sqlite3_serialize(db, schema, pSize, 0);
                if (buff.address() == NULL.address()) {
                    throwex_msg("Serialization failed, allocation failed");
                    return null;
                }
                need_free = true;
            }
            long size = pSize.get(JAVA_LONG, 0);

            byte[] jbuff = new byte[Math.toIntExact(size)];
            System.arraycopy(buff.reinterpret(size, arena, null).toArray(JAVA_BYTE), 0, jbuff, 0,
                    Math.toIntExact(size));

            if (need_free) {
                sqlite3_free(buff);
            }
            return jbuff;
        }
    }

    void deserialize(String jschema, byte[] jbuff) throws SQLException {
        MemorySegment db = gethandle();
        if (db.address() == NULL.address()) {
            throwex_db_closed();
            return;
        }

        long size = jbuff.length;
        MemorySegment sqlite_buff = sqlite3_malloc64(size);
        if (sqlite_buff.address() == NULL.address()) {
            throwex_msg("Failed to allocate native memory for database");
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buff = arena.allocateFrom(JAVA_BYTE, jbuff);
            MemorySegment pSqliteBuff = sqlite_buff.reinterpret(size, arena, null);
            pSqliteBuff.copyFrom(buff);

            // FIXME: may fail if jschema contains null characters! JNI GetStringUTFChars
            MemorySegment schema = arena.allocateFrom(jschema, StandardCharsets.UTF_8);
            int ret = sqlite3_deserialize(db, schema, pSqliteBuff, size, size,
                    SQLITE_DESERIALIZE_FREEONCLOSE | SQLITE_DESERIALIZE_RESIZEABLE);
            if (ret != SQLITE_OK) {
                throwex_errorcode(ret);
            } else {
                MemorySegment max_size = arena.allocate(ADDRESS.withTargetLayout(JAVA_LONG));
                max_size.set(JAVA_LONG, 0, 1024L * 1024L * 1000L * 2L);
                sqlite3_file_control(db, schema, SQLITE_FCNTL_SIZE_LIMIT, max_size);
            }
        }
    }

    private MemorySegment gethandle() {
        return dbpointer;
    }

    private void sethandle(MemorySegment ref) {
        dbpointer = ref;
    }

    private void throwex() throws SQLException {
        $this.throwex();
    }

    private void throwex_errorcode(int errorCode) throws SQLException {
        $this.throwex(errorCode);
    }

    private void throwex_msg(String msg) throws SQLException {
        NativeDB.throwex(msg);
    }

    private void throwex_outofmemory() throws SQLException {
        throwex_msg("Out of memory");
    }

    private void throwex_stmt_finalized() throws SQLException {
        throwex_msg("The prepared statement has been finalized");
    }

    private void throwex_db_closed() throws SQLException {
        throwex_msg("The database has been closed");
    }

    private MemorySegment tovalue(Function function, int arg) throws SQLException {
        MemorySegment value_pntr;
        int numArgs;

        if (arg < 0) {
            throwex_msg("negative arg out of range");
            return NULL;
        }
        if (function == null) {
            throwex_msg("inconsistent function");
            return NULL;
        }

        value_pntr = function._getValue();
        numArgs = function._getArgs();

        if (value_pntr.address() == NULL.address()) {
            throwex_msg("no current value");
            return NULL;
        }
        if (arg >= numArgs) {
            throwex_msg("arg out of range");
            return NULL;
        }

        try (Arena arena = Arena.ofConfined()) {
            return value_pntr.reinterpret(ADDRESS.byteSize() * numArgs, arena, null)
                    .getAtIndex(ADDRESS, arg);
        }
    }

    private void change_progress_handler(ProgressHandler progressHandler, int vmCalls) throws SQLException {
        MemorySegment db;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return;
        }

        if ($this.progressHandlerArena != null) $this.progressHandlerArena.close();
        if (progressHandler != null) {
            $this.progressHandlerArena = Arena.ofShared();
            MemorySegment progress_handler_function = getUpcallStub(
                    (progress_handler_function) _ -> progressHandler._progress(), $this.progressHandlerArena);
            sqlite3_progress_handler(gethandle(), vmCalls, progress_handler_function, NULL);
        } else {
            $this.progressHandlerArena = null;
            sqlite3_progress_handler(gethandle(), 0, NULL, NULL);
        }
    }

    private void change_busy_handler(BusyHandler busyHandler) throws SQLException {
        MemorySegment db;

        db = gethandle();
        if (db == null) {
            throwex_db_closed();
            return;
        }

        if ($this.busyHandlerArena != null) $this.busyHandlerArena.close();
        if (busyHandler != null) {
            $this.busyHandlerArena = Arena.ofShared();
            MemorySegment busyHandlerCallBack = getUpcallStub(
                    (busyHandlerCallBack) (_, nbPrevInvok) -> busyHandler._callback(nbPrevInvok),
                    $this.busyHandlerArena);
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

    private void clear_update_listener() {
        sqlite3_update_hook(gethandle(), NULL, NULL);
    }

    // TODO: rewrite with less reflection
    private void xCall(MemorySegment context, int args, MemorySegment value, Function func, Method method) {
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
        String msg;
        MemorySegment msg_bytes;
        int msg_nbytes;

        msg = ex.toString();

        try (Arena arena = Arena.ofConfined()) {
            msg_bytes = arena.allocateFrom(msg, StandardCharsets.UTF_8);
            msg_nbytes = Math.toIntExact(msg_bytes.elements(JAVA_BYTE).count());

            sqlite3_result_error(context, msg_bytes, msg_nbytes);
        }
    }

    private void copyLoop(MemorySegment pBackup, ProgressObserver progress, int pagesPerStep, int nTimeoutLimit,
                          int sleepTimeMillis) {
        int rc;
        int nTimeout = 0;

        do {
            rc = sqlite3_backup_step(pBackup, pagesPerStep);

            if (rc == SQLITE_OK || rc == SQLITE_DONE) {
                updateProgress(pBackup, progress);
            }

            if (rc == SQLITE_BUSY || rc == SQLITE_LOCKED) {
                if (nTimeout++ >= nTimeoutLimit) break;
                sqlite3_sleep(sleepTimeMillis);
            }
        } while (rc == SQLITE_OK || rc == SQLITE_BUSY || rc == SQLITE_LOCKED);
    }

    private void updateProgress(MemorySegment pBackup, ProgressObserver progress) {
        if (progress == null) return;
        int remaining = sqlite3_backup_remaining(pBackup);
        int pagecount = sqlite3_backup_pagecount(pBackup);
        progress.progress(remaining, pagecount);
    }

    private interface UpcallStub {
        FunctionDescriptor descriptor();
    }

    @FunctionalInterface
    private interface xFunc extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xStep extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xInverse extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() uses via reflection
        void call(MemorySegment context, int args, MemorySegment value) throws SQLException;
    }

    @FunctionalInterface
    private interface xValue extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() uses via reflection
        void call(MemorySegment context) throws SQLException;
    }

    @FunctionalInterface
    private interface xFinal extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        void call(MemorySegment context) throws SQLException;
    }

    @FunctionalInterface
    private interface xCompare extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        int call(MemorySegment context, int len1, MemorySegment str1, int len2, MemorySegment str2);
    }

    @FunctionalInterface
    private interface commit_hook extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        int call(MemorySegment pArg);
    }

    @FunctionalInterface
    private interface rollback_hook extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        void call(MemorySegment pArg);
    }

    @FunctionalInterface
    private interface update_hook extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        void call(MemorySegment context, int type, MemorySegment database, MemorySegment table, long row);
    }

    @FunctionalInterface
    private interface progress_handler_function extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        int call(MemorySegment ctx) throws SQLException;
    }

    @FunctionalInterface
    private interface busyHandlerCallBack extends UpcallStub {
        default FunctionDescriptor descriptor() {
            return FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
        }

        @SuppressWarnings("unused")
            // UpcallStub.stub() calls via reflection
        int call(MemorySegment callback, int nbPrevInvok) throws SQLException;
    }
}
