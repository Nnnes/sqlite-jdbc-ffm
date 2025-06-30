package org.sqlite.core;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/// FFM interface with `sqlite3` native library. Only functions and constants used in [NativeDB_c] have been included.
class sqlite_h {
    static final long SQLITE_TRANSIENT = -1;
    static final int SQLITE_UTF8 = 1;
    static final int SQLITE_UTF16 = 4;
    static final int SQLITE_OPEN_READONLY = 0x00000001;
    static final int SQLITE_OPEN_READWRITE = 0x00000002;
    static final int SQLITE_OPEN_CREATE = 0x00000004;
    static final int SQLITE_OPEN_URI = 0x00000040;
    static final int SQLITE_SERIALIZE_NOCOPY = 0x001;
    static final int SQLITE_DESERIALIZE_FREEONCLOSE = 1;
    static final int SQLITE_DESERIALIZE_RESIZEABLE = 2;
    static final int SQLITE_FCNTL_SIZE_LIMIT = 36;
    private static final MethodHandle sqlite3_backup_finish;
    private static final MethodHandle sqlite3_backup_init;
    private static final MethodHandle sqlite3_backup_pagecount;
    private static final MethodHandle sqlite3_backup_remaining;
    private static final MethodHandle sqlite3_backup_step;
    private static final MethodHandle sqlite3_bind_blob;
    private static final MethodHandle sqlite3_bind_double;
    private static final MethodHandle sqlite3_bind_int;
    private static final MethodHandle sqlite3_bind_int64;
    private static final MethodHandle sqlite3_bind_null;
    private static final MethodHandle sqlite3_bind_parameter_count;
    private static final MethodHandle sqlite3_bind_text;
    private static final MethodHandle sqlite3_busy_handler;
    private static final MethodHandle sqlite3_busy_timeout;
    private static final MethodHandle sqlite3_changes64;
    private static final MethodHandle sqlite3_clear_bindings;
    private static final MethodHandle sqlite3_close;
    private static final MethodHandle sqlite3_column_blob;
    private static final MethodHandle sqlite3_column_bytes;
    private static final MethodHandle sqlite3_column_count;
    private static final MethodHandle sqlite3_column_decltype;
    private static final MethodHandle sqlite3_column_double;
    private static final MethodHandle sqlite3_column_int;
    private static final MethodHandle sqlite3_column_int64;
    private static final MethodHandle sqlite3_column_name;
    private static final MethodHandle sqlite3_column_table_name;
    private static final MethodHandle sqlite3_column_text;
    private static final MethodHandle sqlite3_column_type;
    private static final MethodHandle sqlite3_commit_hook;
    private static final MethodHandle sqlite3_create_collation;
    private static final MethodHandle sqlite3_create_collation_v2;
    private static final MethodHandle sqlite3_create_function;
    private static final MethodHandle sqlite3_create_function_v2;
    private static final MethodHandle sqlite3_create_window_function;
    private static final MethodHandle sqlite3_deserialize;
    private static final MethodHandle sqlite3_enable_load_extension;
    private static final MethodHandle sqlite3_enable_shared_cache;
    private static final MethodHandle sqlite3_errcode;
    private static final MethodHandle sqlite3_errmsg;
    private static final MethodHandle sqlite3_exec;
    private static final MethodHandle sqlite3_extended_errcode;
    private static final MethodHandle sqlite3_extended_result_codes;
    private static final MethodHandle sqlite3_file_control;
    private static final MethodHandle sqlite3_finalize;
    private static final MethodHandle sqlite3_free;
    private static final MethodHandle sqlite3_interrupt;
    private static final MethodHandle sqlite3_libversion;
    private static final MethodHandle sqlite3_libversion_number;
    private static final MethodHandle sqlite3_limit;
    private static final MethodHandle sqlite3_malloc64;
    private static final MethodHandle sqlite3_open_v2;
    private static final MethodHandle sqlite3_prepare_v2;
    private static final MethodHandle sqlite3_progress_handler;
    private static final MethodHandle sqlite3_reset;
    private static final MethodHandle sqlite3_result_blob;
    private static final MethodHandle sqlite3_result_double;
    private static final MethodHandle sqlite3_result_error;
    private static final MethodHandle sqlite3_result_int;
    private static final MethodHandle sqlite3_result_int64;
    private static final MethodHandle sqlite3_result_null;
    private static final MethodHandle sqlite3_result_text;
    private static final MethodHandle sqlite3_rollback_hook;
    private static final MethodHandle sqlite3_serialize;
    private static final MethodHandle sqlite3_sleep;
    private static final MethodHandle sqlite3_step;
    private static final MethodHandle sqlite3_strnicmp;
    private static final MethodHandle sqlite3_table_column_metadata;
    private static final MethodHandle sqlite3_total_changes64;
    private static final MethodHandle sqlite3_update_hook;
    private static final MethodHandle sqlite3_value_blob;
    private static final MethodHandle sqlite3_value_bytes;
    private static final MethodHandle sqlite3_value_double;
    private static final MethodHandle sqlite3_value_int;
    private static final MethodHandle sqlite3_value_int64;
    private static final MethodHandle sqlite3_value_text;
    private static final MethodHandle sqlite3_value_type;

    static {
        SymbolLookup libsqlite3 = SymbolLookup.libraryLookup(System.mapLibraryName("sqlite3"), Arena.ofAuto());

        sqlite3_backup_finish = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_backup_finish"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_backup_init = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_backup_init"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_backup_pagecount = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_backup_pagecount"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_backup_remaining = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_backup_remaining"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_backup_step = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_backup_step"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_bind_blob = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_blob"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_bind_double = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_double"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, JAVA_DOUBLE
                ));
        sqlite3_bind_int = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_int"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, JAVA_INT
                ));
        sqlite3_bind_int64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_int64"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, JAVA_LONG
                ));
        sqlite3_bind_null = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_null"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_bind_parameter_count = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_parameter_count"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_bind_text = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_bind_text"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_busy_handler = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_busy_handler"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_busy_timeout = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_busy_timeout"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_changes64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_changes64"),
                FunctionDescriptor.of(
                        JAVA_LONG,
                        ADDRESS
                ));
        sqlite3_clear_bindings = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_clear_bindings"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_close = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_close"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_column_blob = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_blob"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_bytes = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_bytes"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_count = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_count"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_column_decltype = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_decltype"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_double = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_double"),
                FunctionDescriptor.of(
                        JAVA_DOUBLE,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_int = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_int"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_int64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_int64"),
                FunctionDescriptor.of(
                        JAVA_LONG,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_name = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_name"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_table_name = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_table_name"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_text = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_text"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_column_type = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_column_type"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_commit_hook = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_commit_hook"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_create_collation = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_create_collation"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS
                ));
        sqlite3_create_collation_v2 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_create_collation_v2"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_create_function = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_create_function"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_create_function_v2 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_create_function_v2"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_create_window_function = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_create_window_function"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_deserialize = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_deserialize"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_INT
                ));
        sqlite3_enable_load_extension = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_enable_load_extension"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_enable_shared_cache = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_enable_shared_cache"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        JAVA_INT
                ));
        sqlite3_errcode = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_errcode"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_errmsg = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_errmsg"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS
                ));
        sqlite3_exec = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_exec"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_extended_errcode = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_extended_errcode"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_extended_result_codes = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_extended_result_codes"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT
                ));
        sqlite3_file_control = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_file_control"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_finalize = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_finalize"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_free = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_free"),
                FunctionDescriptor.ofVoid(
                        ADDRESS
                ));
        sqlite3_interrupt = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_interrupt"),
                FunctionDescriptor.ofVoid(
                        ADDRESS
                ));
        sqlite3_libversion = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_libversion"),
                FunctionDescriptor.of(
                        ADDRESS
                ));
        sqlite3_libversion_number = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_libversion_number"),
                FunctionDescriptor.of(
                        JAVA_INT
                ));
        sqlite3_limit = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_limit"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, JAVA_INT, JAVA_INT
                ));
        sqlite3_malloc64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_malloc64"),
                FunctionDescriptor.of(
                        ADDRESS,
                        JAVA_LONG
                ));
        sqlite3_open_v2 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_open_v2"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_prepare_v2 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_prepare_v2"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS
                ));
        sqlite3_progress_handler = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_progress_handler"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, JAVA_INT, ADDRESS, ADDRESS
                ));
        sqlite3_reset = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_reset"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_result_blob = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_blob"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_result_double = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_double"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, JAVA_DOUBLE
                ));
        sqlite3_result_error = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_error"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, ADDRESS, JAVA_INT
                ));
        sqlite3_result_int = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_int"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, JAVA_INT
                ));
        sqlite3_result_int64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_int64"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, JAVA_LONG
                ));
        sqlite3_result_null = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_null"),
                FunctionDescriptor.ofVoid(
                        ADDRESS
                ));
        sqlite3_result_text = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_result_text"),
                FunctionDescriptor.ofVoid(
                        ADDRESS, ADDRESS, JAVA_INT, ADDRESS
                ));
        sqlite3_rollback_hook = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_rollback_hook"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_serialize = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_serialize"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS, JAVA_INT
                ));
        sqlite3_sleep = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_sleep"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        JAVA_INT
                ));
        sqlite3_step = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_step"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_strnicmp = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_strnicmp"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_INT
                ));
        sqlite3_table_column_metadata = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_table_column_metadata"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS
                ));
        sqlite3_total_changes64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_total_changes64"),
                FunctionDescriptor.of(
                        JAVA_LONG,
                        ADDRESS
                ));
        sqlite3_update_hook = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_update_hook"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS
                )
        );
        sqlite3_value_blob = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_blob"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS
                ));
        sqlite3_value_bytes = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_bytes"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_value_double = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_double"),
                FunctionDescriptor.of(
                        JAVA_DOUBLE,
                        ADDRESS
                ));
        sqlite3_value_int = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_int"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
        sqlite3_value_int64 = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_int64"),
                FunctionDescriptor.of(
                        JAVA_LONG,
                        ADDRESS
                ));
        sqlite3_value_text = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_text"),
                FunctionDescriptor.of(
                        ADDRESS,
                        ADDRESS
                ));
        sqlite3_value_type = Linker.nativeLinker().downcallHandle(
                libsqlite3.findOrThrow("sqlite3_value_type"),
                FunctionDescriptor.of(
                        JAVA_INT,
                        ADDRESS
                ));
    }

    static int sqlite3_backup_finish(MemorySegment p) {
        try {
            return (int) sqlite3_backup_finish.invokeExact(p);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_backup_init(MemorySegment pDestDb,
                                             MemorySegment zDestDb,
                                             MemorySegment pSrcDb,
                                             MemorySegment zSrcDb) {
        try {
            return (MemorySegment) sqlite3_backup_init.invokeExact(pDestDb, zDestDb, pSrcDb, zSrcDb);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_backup_pagecount(MemorySegment p) {
        try {
            return (int) sqlite3_backup_pagecount.invokeExact(p);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_backup_remaining(MemorySegment p) {
        try {
            return (int) sqlite3_backup_remaining.invokeExact(p);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_backup_step(MemorySegment p, int nPage) {
        try {
            return (int) sqlite3_backup_step.invokeExact(p, nPage);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_blob(MemorySegment pStmt, int i, MemorySegment zData, int nData, MemorySegment xDel) {
        try {
            return (int) sqlite3_bind_blob.invokeExact(pStmt, i, zData, nData, xDel);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_double(MemorySegment pStmt, int i, double rValue) {
        try {
            return (int) sqlite3_bind_double.invokeExact(pStmt, i, rValue);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_int(MemorySegment p, int i, int iValue) {
        try {
            return (int) sqlite3_bind_int.invokeExact(p, i, iValue);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_int64(MemorySegment pStmt, int i, long iValue) {
        try {
            return (int) sqlite3_bind_int64.invokeExact(pStmt, i, iValue);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_null(MemorySegment pStmt, int i) {
        try {
            return (int) sqlite3_bind_null.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_parameter_count(MemorySegment pStmt) {
        try {
            return (int) sqlite3_bind_parameter_count.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_bind_text(MemorySegment pStmt, int i, MemorySegment zData, int nData, MemorySegment xDel) {
        try {
            return (int) sqlite3_bind_text.invokeExact(pStmt, i, zData, nData, xDel);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_busy_handler(MemorySegment db, MemorySegment xBusy, MemorySegment pArg) {
        try {
            return (int) sqlite3_busy_handler.invokeExact(db, xBusy, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_busy_timeout(MemorySegment db, int ms) {
        try {
            return (int) sqlite3_busy_timeout.invokeExact(db, ms);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static long sqlite3_changes64(MemorySegment db) {
        try {
            return (long) sqlite3_changes64.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_clear_bindings(MemorySegment pStmt) {
        try {
            return (int) sqlite3_clear_bindings.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_close(MemorySegment db) {
        try {
            return (int) sqlite3_close.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_column_blob(MemorySegment pStmt, int i) {
        try {
            return (MemorySegment) sqlite3_column_blob.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_column_bytes(MemorySegment pStmt, int i) {
        try {
            return (int) sqlite3_column_bytes.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_column_count(MemorySegment pStmt) {
        try {
            return (int) sqlite3_column_count.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_column_decltype(MemorySegment pStmt, int N) {
        try {
            return (MemorySegment) sqlite3_column_decltype.invokeExact(pStmt, N);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static double sqlite3_column_double(MemorySegment pStmt, int i) {
        try {
            return (double) sqlite3_column_double.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_column_int(MemorySegment pStmt, int i) {
        try {
            return (int) sqlite3_column_int.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static long sqlite3_column_int64(MemorySegment pStmt, int i) {
        try {
            return (long) sqlite3_column_int64.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_column_name(MemorySegment pStmt, int N) {
        try {
            return (MemorySegment) sqlite3_column_name.invokeExact(pStmt, N);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_column_table_name(MemorySegment pStmt, int N) {
        try {
            return (MemorySegment) sqlite3_column_table_name.invokeExact(pStmt, N);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_column_text(MemorySegment pStmt, int i) {
        try {
            return (MemorySegment) sqlite3_column_text.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_column_type(MemorySegment pStmt, int i) {
        try {
            return (int) sqlite3_column_type.invokeExact(pStmt, i);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_commit_hook(MemorySegment db, MemorySegment xCallback, MemorySegment pArg) {
        try {
            return (MemorySegment) sqlite3_commit_hook.invokeExact(db, xCallback, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_create_collation(MemorySegment db,
                                        MemorySegment zName,
                                        int enc,
                                        MemorySegment pCtx,
                                        MemorySegment xCompare) {
        try {
            return (int) sqlite3_create_collation.invokeExact(db, zName, enc, pCtx, xCompare);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_create_collation_v2(MemorySegment db,
                                           MemorySegment zName,
                                           int enc,
                                           MemorySegment pCtx,
                                           MemorySegment xCompare,
                                           MemorySegment xDel) {
        try {
            return (int) sqlite3_create_collation_v2.invokeExact(db, zName, enc, pCtx, xCompare, xDel);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_create_function(MemorySegment db,
                                       MemorySegment zFunctionName,
                                       int nArg,
                                       int enc,
                                       MemorySegment p,
                                       MemorySegment xSFunc,
                                       MemorySegment xStep,
                                       MemorySegment xFinal) {
        try {
            return (int) sqlite3_create_function.invokeExact(db, zFunctionName, nArg, enc, p, xSFunc, xStep, xFinal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_create_function_v2(MemorySegment db,
                                          MemorySegment zFunc,
                                          int nArg,
                                          int enc,
                                          MemorySegment p,
                                          MemorySegment xSFunc,
                                          MemorySegment xStep,
                                          MemorySegment xFinal,
                                          MemorySegment xDestroy) {
        try {
            return (int) sqlite3_create_function_v2.invokeExact(db,
                    zFunc,
                    nArg,
                    enc,
                    p,
                    xSFunc,
                    xStep,
                    xFinal,
                    xDestroy);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_create_window_function(MemorySegment db,
                                              MemorySegment zFunc,
                                              int nArg,
                                              int enc,
                                              MemorySegment p,
                                              MemorySegment xStep,
                                              MemorySegment xFinal,
                                              MemorySegment xValue,
                                              MemorySegment xInverse,
                                              MemorySegment xDestroy) {
        try {
            return (int) sqlite3_create_window_function.invokeExact(db,
                    zFunc,
                    nArg,
                    enc,
                    p,
                    xStep,
                    xFinal,
                    xValue,
                    xInverse,
                    xDestroy);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_deserialize(MemorySegment db,
                                   MemorySegment zSchema,
                                   MemorySegment pData,
                                   long szDb,
                                   long szBuf,
                                   int mFlags) {
        try {
            return (int) sqlite3_deserialize.invokeExact(db, zSchema, pData, szDb, szBuf, mFlags);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_enable_load_extension(MemorySegment db, int onoff) {
        try {
            return (int) sqlite3_enable_load_extension.invokeExact(db, onoff);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_enable_shared_cache(int enable) {
        try {
            return (int) sqlite3_enable_shared_cache.invokeExact(enable);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_errcode(MemorySegment db) {
        try {
            return (int) sqlite3_errcode.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_errmsg(MemorySegment db) {
        try {
            return (MemorySegment) sqlite3_errmsg.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_exec(MemorySegment db,
                            MemorySegment zSql,
                            MemorySegment xCallback,
                            MemorySegment pArg,
                            MemorySegment pzErrMsg) {
        try {
            return (int) sqlite3_exec.invokeExact(db, zSql, xCallback, pArg, pzErrMsg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_extended_errcode(MemorySegment db) {
        try {
            return (int) sqlite3_extended_errcode.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_extended_result_codes(MemorySegment db, int onoff) {
        try {
            return (int) sqlite3_extended_result_codes.invokeExact(db, onoff);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_file_control(MemorySegment db, MemorySegment zDbName, int op, MemorySegment pArg) {
        try {
            return (int) sqlite3_file_control.invokeExact(db, zDbName, op, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_finalize(MemorySegment pStmt) {
        try {
            return (int) sqlite3_finalize.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_free(MemorySegment p) {
        try {
            sqlite3_free.invokeExact(p);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_interrupt(MemorySegment db) {
        try {
            sqlite3_interrupt.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_libversion() {
        try {
            return (MemorySegment) sqlite3_libversion.invokeExact();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_libversion_number() {
        try {
            return (int) sqlite3_libversion_number.invokeExact();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_limit(MemorySegment db, int limitId, int newLimit) {
        try {
            return (int) sqlite3_limit.invokeExact(db, limitId, newLimit);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_malloc64(long n) {
        try {
            return (MemorySegment) sqlite3_malloc64.invokeExact(n);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_open_v2(MemorySegment filename, MemorySegment ppdb, int flags, MemorySegment zVfs) {
        try {
            return (int) sqlite3_open_v2.invokeExact(filename, ppdb, flags, zVfs);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_prepare_v2(MemorySegment db,
                                  MemorySegment zSql,
                                  int nBytes,
                                  MemorySegment ppStmt,
                                  MemorySegment pzTail) {
        try {
            return (int) sqlite3_prepare_v2.invokeExact(db, zSql, nBytes, ppStmt, pzTail);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_progress_handler(MemorySegment db, int nOps, MemorySegment xProgress, MemorySegment pArg) {
        try {
            sqlite3_progress_handler.invokeExact(db, nOps, xProgress, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_reset(MemorySegment pStmt) {
        try {
            return (int) sqlite3_reset.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_blob(MemorySegment pCtx, MemorySegment z, int n, MemorySegment xDel) {
        try {
            sqlite3_result_blob.invokeExact(pCtx, z, n, xDel);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_double(MemorySegment pCtx, double rVal) {
        try {
            sqlite3_result_double.invokeExact(pCtx, rVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_error(MemorySegment pCtx, MemorySegment z, int n) {
        try {
            sqlite3_result_error.invokeExact(pCtx, z, n);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_int(MemorySegment pCtx, int iVal) {
        try {
            sqlite3_result_int.invokeExact(pCtx, iVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_int64(MemorySegment pCtx, long iVal) {
        try {
            sqlite3_result_int64.invokeExact(pCtx, iVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_null(MemorySegment pCtx) {
        try {
            sqlite3_result_null.invokeExact(pCtx);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static void sqlite3_result_text(MemorySegment pCtx, MemorySegment z, int n, MemorySegment xDel) {
        try {
            sqlite3_result_text.invokeExact(pCtx, z, n, xDel);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_rollback_hook(MemorySegment db, MemorySegment xCallback, MemorySegment pArg) {
        try {
            return (MemorySegment) sqlite3_rollback_hook.invokeExact(db, xCallback, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_serialize(MemorySegment db, MemorySegment zSchema, MemorySegment piSize, int mFlags) {
        try {
            return (MemorySegment) sqlite3_serialize.invokeExact(db, zSchema, piSize, mFlags);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_sleep(int ms) {
        try {
            return (int) sqlite3_sleep.invokeExact(ms);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_step(MemorySegment pStmt) {
        try {
            return (int) sqlite3_step.invokeExact(pStmt);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_strnicmp(MemorySegment zLeft, MemorySegment zRight, int N) {
        try {
            return (int) sqlite3_strnicmp.invokeExact(zLeft, zRight, N);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_table_column_metadata(MemorySegment db,
                                             MemorySegment zDbName,
                                             MemorySegment zTableName,
                                             MemorySegment zColumnName,
                                             MemorySegment pzDataType,
                                             MemorySegment pzCollSeq,
                                             MemorySegment pNotNull,
                                             MemorySegment pPrimaryKey,
                                             MemorySegment pAutoinc) {
        try {
            return (int) sqlite3_table_column_metadata.invokeExact(db,
                    zDbName,
                    zTableName,
                    zColumnName,
                    pzDataType,
                    pzCollSeq,
                    pNotNull,
                    pPrimaryKey,
                    pAutoinc);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static long sqlite3_total_changes64(MemorySegment db) {
        try {
            return (long) sqlite3_total_changes64.invokeExact(db);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_update_hook(MemorySegment db, MemorySegment xCallback, MemorySegment pArg) {
        try {
            return (MemorySegment) sqlite3_update_hook.invokeExact(db, xCallback, pArg);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_value_blob(MemorySegment pVal) {
        try {
            return (MemorySegment) sqlite3_value_blob.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_value_bytes(MemorySegment pVal) {
        try {
            return (int) sqlite3_value_bytes.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static double sqlite3_value_double(MemorySegment pVal) {
        try {
            return (double) sqlite3_value_double.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_value_int(MemorySegment pVal) {
        try {
            return (int) sqlite3_value_int.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static long sqlite3_value_int64(MemorySegment pVal) {
        try {
            return (long) sqlite3_value_int64.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static MemorySegment sqlite3_value_text(MemorySegment pVal) {
        try {
            return (MemorySegment) sqlite3_value_text.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    static int sqlite3_value_type(MemorySegment pVal) {
        try {
            return (int) sqlite3_value_type.invokeExact(pVal);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}
