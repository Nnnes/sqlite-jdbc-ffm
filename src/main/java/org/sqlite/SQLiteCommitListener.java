package org.sqlite;

/**
 * <a
 * href="https://www.sqlite.org/c3ref/commit_hook.html">https://www.sqlite.org/c3ref/commit_hook.html</a>
 */
public interface SQLiteCommitListener {

    void onCommit();

    void onRollback();
}
