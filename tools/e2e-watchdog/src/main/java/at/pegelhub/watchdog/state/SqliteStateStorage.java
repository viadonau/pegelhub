package at.pegelhub.watchdog.state;

import at.pegelhub.watchdog.Json;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SQLite mechanics for bounded JSON documents, without interpreting watchdog state.
 * The single monitoring loop owns all writable operations, including entire transaction callbacks.
 * Diagnostic snapshots use an independent read-only connection and never claim writer ownership.
 *
 * <p>The state directory contains {@code watchdog.db} with this schema, plus {@code owner.lock}
 * for exclusive process ownership (the lock is not a database row):
 * <pre>{@code
 * PRAGMA user_version = 2;
 * CREATE TABLE document (key TEXT PRIMARY KEY, body TEXT NOT NULL);
 * }</pre>
 * Each body is one JSON value: a string timestamp/binding or a structured object. Updates replace
 * the row for that key. {@link StateStore} documents the keys, field meanings and an example snapshot.
 */
final class SqliteStateStorage implements AutoCloseable {
    private static final int SCHEMA_VERSION = 2;

    private final FileChannel lockChannel;
    private final FileLock writerLock;
    private final Connection connection;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final boolean existedOnOpen;

    record Snapshot(int schemaVersion, boolean owned, Map<String, String> documents) {
    }

    SqliteStateStorage(Path directory) throws IOException {
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(
                directory.resolve("owner.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquiredLock = null;
        Connection openedConnection = null;
        try {
            // Claim the directory before opening SQLite: two writers must never share this state.
            acquiredLock = acquireWriterLock(lockChannel);

            Path database = directory.resolve("watchdog.db");
            existedOnOpen = Files.exists(database);
            openedConnection = DriverManager.getConnection("jdbc:sqlite:" + database);
            connection = openedConnection;
            writerLock = acquiredLock;

            // This class owns the physical connection; Spring borrows it without closing it between calls.
            var dataSource = new SingleConnectionDataSource(connection, true);
            jdbc = JdbcClient.create(dataSource);
            transactions = createTransactionTemplate(dataSource);
            initializeDatabase();
        } catch (Exception failure) {
            closeCollectingFailures(openedConnection, failure);
            closeCollectingFailures(acquiredLock, failure);
            closeCollectingFailures(lockChannel, failure);
            throw new IOException("Cannot open watchdog storage (lock, schema, or database failure)", failure);
        }
    }

    boolean existedOnOpen() {
        return existedOnOpen;
    }

    <T> T get(String key, Class<T> type) {
        String body = jdbc.sql("SELECT body FROM document WHERE key = :key")
                .param("key", key)
                .query(String.class)
                .optional().orElse(null);
        return Json.CODEC.fromJson(body, type);
    }

    void put(String key, Object value) {
        jdbc.sql("""
                INSERT INTO document (key, body) VALUES (:key, :body)
                ON CONFLICT(key) DO UPDATE SET body = excluded.body
                """)
                .param("key", key)
                .param("body", Json.CODEC.toJson(value))
                .update();
    }

    void remove(String key) {
        jdbc.sql("DELETE FROM document WHERE key = :key").param("key", key).update();
    }

    void transaction(Runnable work) {
        transactions.executeWithoutResult(status -> work.run());
    }

    <T> T transaction(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }

    /** Missing state is an error, not a reason to create a database from a diagnostic command. */
    static Snapshot inspect(Path directory) throws Exception {
        boolean owned = hasActiveOwner(directory);
        String url = "jdbc:sqlite:" + directory.resolve("watchdog.db").toUri() + "?mode=ro";
        try (var connection = DriverManager.getConnection(url)) {
            return readConsistentSnapshot(connection, owned);
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = new IOException("Cannot close state");
        closeCollectingFailures(connection, failure);
        closeCollectingFailures(writerLock, failure);
        closeCollectingFailures(lockChannel, failure);
        if (failure.getSuppressed().length > 0) {
            throw failure;
        }
    }

    private static FileLock acquireWriterLock(FileChannel channel) throws IOException {
        FileLock lock = channel.tryLock();
        if (lock == null) {
            throw new IOException("State directory already in use");
        }
        return lock;
    }

    private void initializeDatabase() {
        jdbc.sql("PRAGMA busy_timeout=5000").query(Integer.class).single();
        jdbc.sql("PRAGMA synchronous=FULL").update();
        validateDatabase();
        jdbc.sql("CREATE TABLE IF NOT EXISTS document (key TEXT PRIMARY KEY, body TEXT NOT NULL)").update();
        jdbc.sql("PRAGMA user_version=" + SCHEMA_VERSION).update();
    }

    private void validateDatabase() {
        if (!"ok".equals(jdbc.sql("PRAGMA quick_check").query(String.class).single())) {
            throw new IllegalStateException("Invalid state");
        }
        if (existedOnOpen) {
            requireCurrentSchema(jdbc);
        }
    }

    private static TransactionTemplate createTransactionTemplate(DataSource dataSource) {
        var manager = new JdbcTransactionManager(dataSource);
        // SQLite leaves a busy COMMIT active. Roll it back before Spring restores auto-commit.
        manager.setRollbackOnCommitFailure(true);
        return new TransactionTemplate(manager);
    }

    private static Snapshot readConsistentSnapshot(Connection connection, boolean owned) {
        var dataSource = new SingleConnectionDataSource(connection, true);
        var jdbc = JdbcClient.create(dataSource);
        // Schema and documents must come from one committed snapshot while the loop keeps writing.
        return createTransactionTemplate(dataSource).execute(status -> {
            requireCurrentSchema(jdbc);
            return new Snapshot(SCHEMA_VERSION, owned, readDocuments(jdbc));
        });
    }

    private static Map<String, String> readDocuments(JdbcClient jdbc) {
        return jdbc.sql("SELECT key, body FROM document").query(rows -> {
            var documents = new LinkedHashMap<String, String>();
            while (rows.next()) {
                documents.put(rows.getString("key"), rows.getString("body"));
            }
            return documents;
        });
    }

    private static void requireCurrentSchema(JdbcClient jdbc) {
        if (jdbc.sql("PRAGMA user_version").query(Integer.class).single() != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported state version");
        }
    }

    private static boolean hasActiveOwner(Path directory) throws IOException {
        try (var channel = FileChannel.open(directory.resolve("owner.lock"), StandardOpenOption.WRITE)) {
            try (var lock = channel.tryLock()) {
                return lock == null;
            } catch (OverlappingFileLockException sameProcess) {
                return true;
            }
        }
    }

    private static void closeCollectingFailures(AutoCloseable resource, Exception failure) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception cleanup) {
            failure.addSuppressed(cleanup);
        }
    }
}
