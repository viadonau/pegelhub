package at.pegelhub.watchdog.state;

import at.pegelhub.watchdog.monitoring.CheckState;
import at.pegelhub.watchdog.monitoring.Checks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;

import static at.pegelhub.watchdog.WatchdogFixtures.START;
import static at.pegelhub.watchdog.WatchdogFixtures.config;
import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {
    @TempDir
    Path directory;

    @Test
    void failedObservationWriteRollsBackItsClockWatermark() throws Exception {
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START);
            var original = Checks.evaluate(START, START, Duration.ofSeconds(600));
            store.record(original);

            // Fail the second write, after record() has already updated the clock watermark.
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("watchdog.db"));
                 var sql = connection.createStatement()) {
                sql.execute("""
                        CREATE TRIGGER reject_check BEFORE UPDATE ON document
                        WHEN NEW.key = 'check'
                        BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END
                        """);
                assertThrows(RuntimeException.class, () -> store.record(
                        Checks.evaluate(START, START.plusSeconds(20), Duration.ofSeconds(600))));
                assertEquals(original, store.check());
                sql.execute("DROP TRIGGER reject_check");
            }

            // This time is before the failed write, so a leaked watermark would reject it.
            var next = Checks.evaluate(START, START.plusSeconds(10), Duration.ofSeconds(600));
            store.record(next);
            assertEquals(next, store.check());
        }

        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            assertEquals(START.plusSeconds(10), store.check().evaluatedAt());
            assertEquals(CheckState.Status.OK, store.check().status());
        }
    }

    @Test
    void failedCommitKeepsPreviousStateAndTheConnectionRemainsUsable() throws Exception {
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START);
            var original = CheckState.unknown("read_failed", START);
            store.record(original);
            store.emitted("receiver", "ERR", START);

            // A separate reader keeps a shared lock: writes can start, but COMMIT must time out.
            try (var reader = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("watchdog.db"));
                 var sql = reader.createStatement()) {
                reader.setAutoCommit(false);
                try (var rows = sql.executeQuery("SELECT body FROM document WHERE key = 'check'")) {
                    assertTrue(rows.next());
                }
                assertThrows(RuntimeException.class, () -> store.record(
                        Checks.evaluate(START, START.plusSeconds(20), Duration.ofSeconds(600))));
                reader.rollback();
            }

            assertEquals(original, store.check());
            assertEquals("ERR", store.publication("receiver").signal());
            var next = Checks.evaluate(START, START.plusSeconds(10), Duration.ofSeconds(600));
            store.record(next);
            assertEquals(next, store.check());
        }
    }

    @Test
    void inspectingMissingStateDoesNotCreateADatabase() throws Exception {
        Files.createFile(directory.resolve("owner.lock"));

        assertThrows(Exception.class, () -> StateStore.inspect(directory, START));

        assertFalse(Files.exists(directory.resolve("watchdog.db")));
    }
}
