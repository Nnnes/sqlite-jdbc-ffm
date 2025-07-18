package org.sqlite;

public enum SQLiteLimits {
    SQLITE_LIMIT_LENGTH(0),
    SQLITE_LIMIT_SQL_LENGTH(1),
    SQLITE_LIMIT_COLUMN(2),
    SQLITE_LIMIT_EXPR_DEPTH(3),
    SQLITE_LIMIT_COMPOUND_SELECT(4),
    SQLITE_LIMIT_VDBE_OP(5),
    SQLITE_LIMIT_FUNCTION_ARG(6),
    SQLITE_LIMIT_ATTACHED(7),
    SQLITE_LIMIT_LIKE_PATTERN_LENGTH(8),
    SQLITE_LIMIT_VARIABLE_NUMBER(9),
    SQLITE_LIMIT_TRIGGER_DEPTH(10),
    SQLITE_LIMIT_WORKER_THREADS(11),
    SQLITE_LIMIT_PAGE_COUNT(12);

    private final int id;

    SQLiteLimits(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
