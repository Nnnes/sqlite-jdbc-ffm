package org.sqlite.core;

import java.lang.foreign.MemorySegment;
import java.sql.SQLException;
import java.util.Objects;

/// A class for safely wrapping calls to a native pointer to a statement, ensuring no other thread
/// has access to the pointer while it is run
///
/// `sqlite-jdbc-ffm`: Extensive changes have been made to this class's method signatures to replace
/// raw memory address handling with FFM [MemorySegment]s.
public class SafeStmtPtr {
    // store a reference to the DB, to lock it before any safe function is called. This avoids
    // deadlocking by locking the DB. All calls with the raw pointer are synchronized with the DB
    // anyways, so making a separate lock would be pointless
    private final DB db;
    private final MemorySegment stmt;

    private volatile boolean closed = false;
    // to return on subsequent calls to close() after this ptr has been closed
    private int closedRC;
    // to throw on subsequent calls to close after this ptr has been closed, if the close function
    // threw an exception
    private SQLException closeException;

    /**
     * Construct a new Safe Pointer Wrapper to ensure a pointer is properly handled
     *
     * @param db the database that made this pointer. Always locked before any safe run function is
     *     executed to avoid deadlocks
     * @param stmt the raw pointer contained in a MemorySegment
     */
    public SafeStmtPtr(DB db, MemorySegment stmt) {
        Objects.requireNonNull(db);
        Objects.requireNonNull(stmt);
        if (stmt.address() == MemorySegment.NULL.address()) throw new NullPointerException();
        this.db = db;
        this.stmt = stmt;
    }

    /**
     * Check whether this pointer has been closed
     *
     * @return whether this pointer has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Close this pointer
     *
     * @return the return code of the close callback function
     * @throws SQLException if the close callback throws an SQLException, or the pointer is locked
     *     elsewhere
     */
    public int close() throws SQLException {
        synchronized (db) {
            return internalClose();
        }
    }

    private int internalClose() throws SQLException {
        try {
            // if this is already closed, return or throw the previous result
            if (closed) {
                if (closeException != null) throw closeException;
                return closedRC;
            }
            closedRC = db.finalize(this, stmt);
            return closedRC;
        } catch (SQLException ex) {
            this.closeException = ex;
            throw ex;
        } finally {
            this.closed = true;
        }
    }

    /**
     * Run a callback with the wrapped pointer safely.
     *
     * @param run the function to run
     * @return the return of the passed in function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> int safeRunInt(SafePtrIntFunction<E> run) throws SQLException, E {
        synchronized (db) {
            this.ensureOpen();
            return run.run(db, stmt);
        }
    }

    /**
     * Run a callback with the wrapped pointer safely.
     *
     * @param run the function to run
     * @return the return of the passed in function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> long safeRunLong(SafePtrLongFunction<E> run)
            throws SQLException, E {
        synchronized (db) {
            this.ensureOpen();
            return run.run(db, stmt);
        }
    }

    /**
     * Run a callback with the wrapped pointer safely.
     *
     * @param run the function to run
     * @return the return of the passed in function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> double safeRunDouble(SafePtrDoubleFunction<E> run)
            throws SQLException, E {
        synchronized (db) {
            this.ensureOpen();
            return run.run(db, stmt);
        }
    }

    /**
     * Run a callback with the wrapped pointer safely.
     *
     * @param run the function to run
     * @return the return code of the function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <T, E extends Throwable> T safeRun(SafePtrFunction<T, E> run) throws SQLException, E {
        synchronized (db) {
            this.ensureOpen();
            return run.run(db, stmt);
        }
    }

    /**
     * Run a callback with the wrapped pointer safely.
     *
     * @param run the function to run
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> void safeRunConsume(SafePtrConsumer<E> run)
            throws SQLException, E {
        synchronized (db) {
            this.ensureOpen();
            run.run(db, stmt);
        }
    }

    private void ensureOpen() throws SQLException {
        if (this.closed) {
            throw new SQLException("stmt pointer is closed");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SafeStmtPtr that = (SafeStmtPtr) o;
        return stmt.address() == that.stmt.address();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(stmt.address());
    }

    @FunctionalInterface
    public interface SafePtrIntFunction<E extends Throwable> {
        int run(DB db, MemorySegment stmt) throws E;
    }

    @FunctionalInterface
    public interface SafePtrLongFunction<E extends Throwable> {
        long run(DB db, MemorySegment stmt) throws E;
    }

    @FunctionalInterface
    public interface SafePtrDoubleFunction<E extends Throwable> {
        double run(DB db, MemorySegment stmt) throws E;
    }

    @FunctionalInterface
    public interface SafePtrFunction<T, E extends Throwable> {
        T run(DB db, MemorySegment stmt) throws E;
    }

    @FunctionalInterface
    public interface SafePtrConsumer<E extends Throwable> {
        void run(DB db, MemorySegment stmt) throws E;
    }
}
