package at.pegelhub.watchdog.monitoring;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import at.pegelhub.watchdog.Json;
import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.state.StateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static at.pegelhub.watchdog.WatchdogFixtures.BACK;
import static at.pegelhub.watchdog.WatchdogFixtures.START;
import static at.pegelhub.watchdog.WatchdogFixtures.config;
import static org.junit.jupiter.api.Assertions.*;

class WatchdogTest {
    @TempDir
    Path directory;

    @Test
    void pollingOldDataDoesNotRenewFreshnessAndANewTimestampRecoversEvenWithTheSameValue() throws Exception {
        var clock = new TestClock();
        var core = new FakeCore();
        core.latest = new Measurement(BACK, START, 281.0);
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(clock.now);
            var watchdog = new Watchdog(config(1162, "v2c"), store, core, clock);
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.OK, store.check().status());

            clock.now = START.plusSeconds(601);
            watchdog.checkFreshness();
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.CRITICAL, store.check().status());

            core.latest = new Measurement(BACK, clock.now, 281.0);
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.OK, store.check().status());
        }
    }

    @Test
    void unavailableReadsNeverClearAnAlarmOrExposeExceptionDetails() throws Exception {
        var clock = new TestClock();
        var core = new FakeCore();
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(clock.now);
            var watchdog = new Watchdog(config(1162, "v2c"), store, core, clock);
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.CRITICAL, store.check().status());

            core.failRead = true;
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.UNKNOWN, store.check().status());
            assertEquals("ERR", store.check().signal());
            assertFalse(Json.CODEC.toJson(StateStore.inspect(directory, clock.now)).contains("private credential"));

            core.failRead = false;
            core.latest = new Measurement(BACK, START.minusSeconds(700), 281.0);
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.CRITICAL, store.check().status());
        }
    }

    @Test
    void changingTheThresholdStillRequiresASuccessfulReadToClearAnAlarm() throws Exception {
        var original = config(1162, "v2c");
        var adjusted = new WatchdogConfig(original.watchdogId(), original.core(), BACK, 900, 1L, original.snmp());
        String receiver = original.snmp().receivers().getFirst().id();
        try (var store = new StateStore(directory, original)) {
            store.beginSession(START);
            store.record(Checks.evaluate(START.minusSeconds(700), START, original.silence()));
            store.emitted(receiver, "ERR", START);
        }

        var clock = new TestClock();
        clock.now = START.plusSeconds(1);
        var core = new FakeCore();
        core.latest = new Measurement(BACK, START.minusSeconds(700), 281.0);
        core.failRead = true;
        try (var store = new StateStore(directory, adjusted)) {
            store.beginSession(clock.now);
            assertEquals("ERR", store.publication(receiver).signal());

            var watchdog = new Watchdog(adjusted, store, core, clock);
            watchdog.checkFreshness();
            assertEquals("ERR", store.check().signal());
            core.failRead = false;
            watchdog.checkFreshness();
            assertEquals(CheckState.Status.OK, store.check().status());
            assertEquals("ERR", store.publication(receiver).signal());
        }
    }

    @Test
    void backwardsClockCannotTurnAnExpiredSampleFreshEvenAcrossRestart() throws Exception {
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START);
            store.record(Checks.evaluate(START, START.plusSeconds(601), Duration.ofSeconds(600)));
            store.record(Checks.evaluate(START, START.plusSeconds(300), Duration.ofSeconds(600)));
            assertEquals("clock_regressed", store.check().reason());
        }
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START.plusSeconds(400));
            store.record(Checks.evaluate(START, START.plusSeconds(400), Duration.ofSeconds(600)));
            assertEquals(CheckState.Status.UNKNOWN, store.check().status());
            store.record(Checks.evaluate(START.plusSeconds(610), START.plusSeconds(610), Duration.ofSeconds(600)));
            assertEquals(CheckState.Status.OK, store.check().status());
        }
    }

    @Test
    void storageIsExclusiveRouteBoundAndHealthMeasuresTheLoopNotTheRoute() throws Exception {
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START);
            store.record(CheckState.unknown("read_failed", START));
            assertThrows(Exception.class, () -> new StateStore(directory, config(1162, "v2c")));
            assertEquals(true, StateStore.inspect(directory, START.plusSeconds(1)).get("healthy"));
            assertEquals(true, StateStore.inspect(directory, START.plusSeconds(179)).get("healthy"));
            assertEquals(false, StateStore.inspect(directory, START.plusSeconds(180)).get("healthy"));
            assertEquals(false, StateStore.inspect(directory, START.plusSeconds(181)).get("healthy"));
            store.progress(START.plusSeconds(181));
            assertEquals(true, StateStore.inspect(directory, START.plusSeconds(181)).get("healthy"));
            store.emissionFailed("receiver");
            assertEquals(false, StateStore.inspect(directory, START.plusSeconds(181)).get("healthy"));
        }
        assertEquals(false, StateStore.inspect(directory, START.plusSeconds(181)).get("healthy"));
        assertThrows(Exception.class, () -> new StateStore(directory, config(1163, "v2c")));
        var original = config(1162, "v2c");
        var otherSeries = new WatchdogConfig(original.watchdogId(), original.core(), UUID.randomUUID(),
                original.silenceSeconds(), original.pollSeconds(), original.snmp());
        assertThrows(Exception.class, () -> new StateStore(directory, otherSeries));
    }

    @Test
    void localEmissionFailureSurvivesRestartAndOnlySuccessfulEmissionRepairsHealth() throws Exception {
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START);
            store.emissionFailed("receiver");
        }
        try (var store = new StateStore(directory, config(1162, "v2c"))) {
            store.beginSession(START.plusSeconds(1));
            store.record(Checks.evaluate(START, START.plusSeconds(1), Duration.ofSeconds(600)));
            assertEquals(false, StateStore.inspect(directory, START.plusSeconds(1)).get("healthy"));
            store.emitted("receiver", "ERR", START.plusSeconds(2));
            assertEquals(true, StateStore.inspect(directory, START.plusSeconds(2)).get("healthy"));
        }
    }

    @Test
    void corruptStorageIsRejectedWithoutReplacingIt() throws Exception {
        var database = directory.resolve("watchdog.db");
        Files.writeString(database, "not SQLite");
        assertThrows(Exception.class, () -> new StateStore(directory, config(1162, "v2c")));
        assertEquals("not SQLite", Files.readString(database));
    }

    @Test
    void blockedHttpLeavesDiagnosticsReadableButCannotRenewLoopHealth() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var reader = new FakeCore() {
            @Override
            public Optional<Measurement> getLatestMeasurementOfTimeSeries(
                    UUID id, MeasurementRepresentation representation) {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }
        };
        try (var store = new StateStore(directory, config(1162, "v2c"));
             var executor = Executors.newSingleThreadExecutor()) {
            store.beginSession(START);
            var watchdog = new Watchdog(config(1162, "v2c"), store, reader, new TestClock());
            var future = executor.submit(watchdog::checkFreshness);
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                        assertEquals(false, StateStore.inspect(directory, START.plusSeconds(181)).get("healthy")));
            } finally {
                release.countDown();
            }
            future.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void invalidConfigIsRejectedWithoutDisclosingYamlSecrets() throws Exception {
        var file = directory.resolve("watchdog.yaml");
        Files.writeString(file, "watchdogId: [private-secret-in-bad-yaml]\n");
        var failure = assertThrows(Exception.class, () -> WatchdogConfig.load(file));
        assertFalse(failure.getMessage().contains("private-secret"));

        var cfg = config(1162, "v3");
        assertThrows(Exception.class, () -> new WatchdogConfig("test", cfg.core(), BACK, 0, 1L, cfg.snmp()));
        assertThrows(Exception.class, () -> new WatchdogConfig.Snmp(List.of(), "v3", "1.3.6.1", "1.3.6.2",
                null, "test", "auth", "privacy"));
    }

    static class TestClock extends Clock {
        Instant now = START;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static class FakeCore implements PegelHubClient {
        Measurement latest;
        boolean failRead;

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(
                UUID id, Instant from, Instant to, MeasurementRepresentation representation) {
            throw new AssertionError("Freshness monitoring needs no window or per-probe history");
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(
                UUID id, MeasurementRepresentation representation) {
            assertEquals(BACK, id);
            if (failRead) {
                throw new IllegalStateException("private credential");
            }
            return Optional.ofNullable(latest);
        }

        @Override
        public void sendMeasurements(List<Measurement> points) {
            throw new AssertionError("The passive watchdog must never write measurements");
        }

        @Override
        public void close() {
        }
    }
}
