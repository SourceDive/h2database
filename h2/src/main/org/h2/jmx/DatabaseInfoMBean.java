/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jmx;

/**
 * Information and management operations for the given database.
 *
 * @author Eric Dong
 * @author Thomas Mueller
 * @h2.resource
 */
public interface DatabaseInfoMBean {

    /**
     * Is the database open in exclusive mode?
     *
     * @return true if the database is open in exclusive mode, false otherwise
     * @h2.resource
     */
    boolean isExclusive();

    /**
     * Is the database read-only?
     *
     * @return true if the database is read-only, false otherwise
     * @h2.resource
     */
    boolean isReadOnly();

    /**
     * The database compatibility mode (REGULAR if no compatibility mode is
     * used).
     *
     * @return the database mode
     * @h2.resource
     */
    String getMode();

    /**
     * Is multi-threading enabled?
     *
     * @return true if multi-threading is enabled, false otherwise
     * @h2.resource
     */
    boolean isMultiThreaded();

    /**
     * Is MVCC (multi version concurrency) enabled?
     *
     * @return true if MVCC is enabled, false otherwise
     * @h2.resource
     */
    boolean isMvcc();

    /**
     * The transaction log mode (0 disabled, 1 without sync, 2 enabled).
     *
     * @return the transaction log mode
     * @h2.resource
     */
    int getLogMode();

    /**
     * Set the transaction log mode.
     *
     * @param value the new log mode
     */
    void setLogMode(int value);

    /**
     * The number of write operations since the database was created.
     *
     * @return the total write count
     * @h2.resource
     */
    long getFileWriteCountTotal();

    /**
     * The number of write operations since the database was opened.
     *
     * @return the write count
     * @h2.resource
     */
    long getFileWriteCount();

    /**
     * The file read count since the database was opened.
     *
     * @return the read count
     * @h2.resource
     */
    long getFileReadCount();

    /**
     * The database file size in KB.
     *
     * @return the number of pages
     * @h2.resource
     */
    long getFileSize();

    /**
     * The maximum cache size in KB.
     *
     * @return the maximum size
     * @h2.resource
     */
    int getCacheSizeMax();

    /**
     * Change the maximum size.
     *
     * @param kb the cache size in KB.
     */
    void setCacheSizeMax(int kb);

    /**
     * The current cache size in KB.
     *
     * @return the current size
     * @h2.resource
     */
    int getCacheSize();

    /**
     * The database version.
     *
     * @return the version
     * @h2.resource
     */
    String getVersion();

    /**
     * The trace level (0 disabled, 1 error, 2 info, 3 debug).
     *
     * @return the level
     * @h2.resource
     */
    int getTraceLevel();

    /**
     * Set the trace level.
     *
     * @param level the new value
     */
    void setTraceLevel(int level);

    /**
     * List the database settings.
     *
     * @return the database settings
     * @h2.resource
     */
    String listSettings();

    /**
     * List sessions, including the queries that are in
     * progress, and locked tables.
     *
     * @return information about the sessions
     * @h2.resource
     */
    String listSessions();

}
