package dao;

/**
 * Database compatibility stub.
 * External SQL databases and SQLite have been replaced with 100% Core Java
 * File I/O persistence in storage.DataManager.
 */
public final class Database {

    private Database() {
    }

    public static synchronized void init() {
        // Core Java File I/O persistence is used via storage.DataManager
    }

    public static synchronized void close() {
        // No external connections to close
    }
}
