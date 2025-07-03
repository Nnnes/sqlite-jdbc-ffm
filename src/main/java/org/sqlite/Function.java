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
package org.sqlite;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.core.Codes;
import org.sqlite.core.DB;

/**
 * Provides an interface for creating SQLite user-defined functions.
 *
 * <p>A subclass of <tt>org.sqlite.Function</tt> can be registered with <tt>Function.create()</tt>
 * and called by the name it was given. All functions must implement <tt>xFunc()</tt>, which is
 * called when SQLite runs the custom function. E.g.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Function.create(conn, "myFunc", new Function() {
 *          protected void xFunc() {
 *              System.out.println("myFunc called!");
 *          }
 *      });
 *
 *      conn.createStatement().execute("select myFunc();");
 *  </pre>
 *
 * <p>Arguments passed to a custom function can be accessed using the <tt>protected</tt> functions
 * provided. <tt>args()</tt> returns the number of arguments passed, while
 * <tt>value_&lt;type&gt;(int)</tt> returns the value of the specific argument. Similarly, a
 * function can return a value using the <tt>result(&lt;type&gt;)</tt> function.
 */
public abstract class Function {
    /**
     * Flag to provide to {@link #create(Connection, String, Function, int)} that marks this
     * Function as deterministic, making is usable in Indexes on Expressions.
     */
    public static final int FLAG_DETERMINISTIC = 0x800;

    private SQLiteConnection conn;
    private DB db;

    MemorySegment pContext = MemorySegment.NULL; // pointer sqlite3_context*
    MemorySegment pValue = MemorySegment.NULL; // pointer sqlite3_value**
    int args = 0;

    public final Arena _arena = Arena.ofAuto();

    public void _setContext(MemorySegment pContext) {
        this.pContext = pContext;
    }

    public MemorySegment _getValue() {
        return pValue;
    }

    public void _setValue(MemorySegment pValue) {
        this.pValue = pValue;
    }

    public int _getArgs() {
        return args;
    }

    public void _setArgs(int args) {
        this.args = args;
    }

    /**
     * Registers a given function with the connection.
     *
     * @param conn The connection.
     * @param name The name of the function.
     * @param f The function to register.
     */
    public static void create(Connection conn, String name, Function f) throws SQLException {
        create(conn, name, f, 0);
    }

    /**
     * Registers a given function with the connection.
     *
     * @param conn The connection.
     * @param name The name of the function.
     * @param f The function to register.
     * @param flags Extra flags to pass, such as {@link #FLAG_DETERMINISTIC}
     */
    public static void create(Connection conn, String name, Function f, int flags)
            throws SQLException {
        create(conn, name, f, -1, flags);
    }

    /**
     * Registers a given function with the connection.
     *
     * @param conn The connection.
     * @param name The name of the function.
     * @param f The function to register.
     * @param nArgs The number of arguments that the function takes.
     * @param flags Extra flags to pass, such as {@link #FLAG_DETERMINISTIC}
     */
    public static void create(Connection conn, String name, Function f, int nArgs, int flags)
            throws SQLException {
        if (!(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }

        f.conn = (SQLiteConnection) conn;
        f.db = f.conn.getDatabase();

        if (nArgs < -1 || nArgs > 127) {
            throw new SQLException("invalid args provided: " + nArgs);
        }

        if (f.db.create_function(name, f, nArgs, flags) != Codes.SQLITE_OK) {
            throw new SQLException("error creating function");
        }
    }

    /**
     * Removes a named function from the given connection.
     *
     * @param conn The connection to remove the function from.
     * @param name The name of the function.
     * @param nArgs Ignored.
     * @throws SQLException
     */
    public static void destroy(Connection conn, String name, int nArgs) throws SQLException {
        if (!(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        ((SQLiteConnection) conn).getDatabase().destroy_function(name);
    }

    /**
     * Removes a named function from the given connection.
     *
     * @param conn The connection to remove the function from.
     * @param name The name of the function.
     * @throws SQLException
     */
    public static void destroy(Connection conn, String name) throws SQLException {
        destroy(conn, name, -1);
    }

    /**
     * Called by SQLite as a custom function. Should access arguments through <tt>value_*(int)</tt>,
     * return results with <tt>result(*)</tt> and throw errors with <tt>error(String)</tt>.
     */
    protected abstract void xFunc() throws SQLException;

    public void _xFunc() throws SQLException {
        xFunc();
    }

    /**
     * Returns the number of arguments passed to the function. Can only be called from
     * <tt>xFunc()</tt>.
     */
    protected final synchronized int args() throws SQLException {
        checkContext();
        return args;
    }

    /**
     * Called by <tt>xFunc</tt> to return a value.
     *
     * @param value
     */
    protected final synchronized void result(byte[] value) throws SQLException {
        checkContext();
        db.result_blob(pContext, value);
    }

    /**
     * Called by <tt>xFunc</tt> to return a value.
     *
     * @param value
     */
    protected final synchronized void result(double value) throws SQLException {
        checkContext();
        db.result_double(pContext, value);
    }

    /**
     * Called by <tt>xFunc</tt> to return a value.
     *
     * @param value
     */
    protected final synchronized void result(int value) throws SQLException {
        checkContext();
        db.result_int(pContext, value);
    }

    /**
     * Called by <tt>xFunc</tt> to return a value.
     *
     * @param value
     */
    protected final synchronized void result(long value) throws SQLException {
        checkContext();
        db.result_long(pContext, value);
    }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected final synchronized void result() throws SQLException {
        checkContext();
        db.result_null(pContext);
    }

    /**
     * Called by <tt>xFunc</tt> to return a value.
     *
     * @param value
     */
    protected final synchronized void result(String value) throws SQLException {
        checkContext();
        db.result_text(pContext, value);
    }

    /**
     * Called by <tt>xFunc</tt> to throw an error.
     *
     * @param err
     */
    protected final synchronized void error(String err) throws SQLException {
        checkContext();
        db.result_error(pContext, err);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized String value_text(int arg) throws SQLException {
        checkValue(arg);
        return db.value_text(this, arg);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized byte[] value_blob(int arg) throws SQLException {
        checkValue(arg);
        return db.value_blob(this, arg);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized double value_double(int arg) throws SQLException {
        checkValue(arg);
        return db.value_double(this, arg);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized int value_int(int arg) throws SQLException {
        checkValue(arg);
        return db.value_int(this, arg);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized long value_long(int arg) throws SQLException {
        checkValue(arg);
        return db.value_long(this, arg);
    }

    /**
     * Called by <tt>xFunc</tt> to access the value of an argument.
     *
     * @param arg
     */
    protected final synchronized int value_type(int arg) throws SQLException {
        checkValue(arg);
        return db.value_type(this, arg);
    }

    /**
     * @throws SQLException
     */
    private void checkContext() throws SQLException {
        if (conn == null
                || conn.getDatabase() == null
                || pContext.address() == MemorySegment.NULL.address()) {
            throw new SQLException("no context, not allowed to read value");
        }
    }

    /**
     * @param arg
     * @throws SQLException
     */
    private void checkValue(int arg) throws SQLException {
        if (conn == null
                || conn.getDatabase() == null
                || pContext.address() == MemorySegment.NULL.address()) {
            throw new SQLException("not in value access state");
        }
        if (arg >= args) {
            throw new SQLException("arg " + arg + " out bounds [0," + args + ")");
        }
    }

    /**
     * Provides an interface for creating SQLite user-defined aggregate functions.
     *
     * @see Function
     */
    public abstract static class Aggregate extends Function implements Cloneable {
        /**
         * @see org.sqlite.Function#xFunc()
         */
        protected final void xFunc() {}

        /**
         * Defines the abstract aggregate callback function
         *
         * @throws SQLException
         * @see <a
         *     href="https://www.sqlite.org/c3ref/aggregate_context.html">https://www.sqlite.org/c3ref/aggregate_context.html</a>
         */
        protected abstract void xStep() throws SQLException;

        public void _xStep() throws SQLException {
            xStep();
        }

        /**
         * Defines the abstract aggregate callback function
         *
         * @throws SQLException
         * @see <a
         *     href="https://www.sqlite.org/c3ref/aggregate_context.html">https://www.sqlite.org/c3ref/aggregate_context.html</a>
         */
        protected abstract void xFinal() throws SQLException;

        public void _xFinal() throws SQLException {
            xFinal();
        }

        /**
         * @see java.lang.Object#clone()
         */
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * Provides an interface for creating SQLite user-defined window functions.
     *
     * @see Aggregate
     */
    public abstract static class Window extends Aggregate {
        /**
         * Defines the abstract window callback function
         *
         * @throws SQLException
         * @see <a
         *     href="https://www.sqlite.org/windowfunctions.html#user_defined_aggregate_window_functions">https://www.sqlite.org/windowfunctions.html#user_defined_aggregate_window_functions</a>
         */
        protected abstract void xInverse() throws SQLException;

        public void _xInverse() throws SQLException {
            xInverse();
        }

        /**
         * Defines the abstract window callback function
         *
         * @throws SQLException
         * @see <a
         *     href="https://www.sqlite.org/windowfunctions.html#user_defined_aggregate_window_functions">https://www.sqlite.org/windowfunctions.html#user_defined_aggregate_window_functions</a>
         */
        protected abstract void xValue() throws SQLException;

        public void _xValue() throws SQLException {
            xValue();
        }
    }
}
