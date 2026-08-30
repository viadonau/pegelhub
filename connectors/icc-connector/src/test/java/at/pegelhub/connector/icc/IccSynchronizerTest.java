package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IccSynchronizerTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXTERNAL_TIME_SERIES_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant SYNC_AT = Instant.parse("2026-06-07T11:00:00Z");

    @Test
    void shouldSyncCoreMeasurementsToExternalCore() {
        List<Measurement> measurements = List.of(new Measurement(TIME_SERIES_ID, Instant.parse("2026-06-07T10:15:30Z"), 42.0));
        FakeCommunicator core = new FakeCommunicator(measurements);
        FakeCommunicator external = new FakeCommunicator(List.of());

        synchronizer(core, external, MappingDirection.CORE_TO_EXTERNAL).run();

        assertEquals(TIME_SERIES_ID, core.requestedTimeSeriesId);
        assertEquals(List.of(new ReadWindow(SYNC_AT.minus(Duration.ofHours(24)), SYNC_AT)), core.requestedWindows);
        assertEquals(1, external.sentMeasurements.size());
        Measurement sent = external.sentMeasurements.getFirst();
        assertEquals(EXTERNAL_TIME_SERIES_ID, sent.getTimeSeriesId());
        assertEquals(Instant.parse("2026-06-07T10:15:30Z"), sent.getObservedAt());
        assertEquals(42.0, sent.getValue());
    }

    @Test
    void shouldSyncExternalMeasurementsToCore() {
        List<Measurement> measurements = List.of(new Measurement(EXTERNAL_TIME_SERIES_ID, Instant.parse("2026-06-07T10:15:30Z"), 42.0));
        FakeCommunicator core = new FakeCommunicator(List.of());
        FakeCommunicator external = new FakeCommunicator(measurements);

        synchronizer(core, external, MappingDirection.EXTERNAL_TO_CORE).run();

        assertEquals(EXTERNAL_TIME_SERIES_ID, external.requestedTimeSeriesId);
        assertEquals(List.of(new ReadWindow(SYNC_AT.minus(Duration.ofHours(24)), SYNC_AT)), external.requestedWindows);
        assertEquals(1, core.sentMeasurements.size());
        Measurement sent = core.sentMeasurements.getFirst();
        assertEquals(TIME_SERIES_ID, sent.getTimeSeriesId());
        assertEquals(Instant.parse("2026-06-07T10:15:30Z"), sent.getObservedAt());
        assertEquals(42.0, sent.getValue());
    }

    @Test
    void shouldSkipMissingTimeSeries() {
        PegelHubClient core = new MissingTimeSeriesCommunicator();
        FakeCommunicator external = new FakeCommunicator(List.of());

        synchronizer(core, external, MappingDirection.CORE_TO_EXTERNAL).run();

        assertTrue(external.sentMeasurements.isEmpty());
    }

    @Test
    void retriesTheSameStartBoundaryAfterTargetFailure() {
        Instant nextCycle = SYNC_AT.plus(Duration.ofMinutes(10));
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(SYNC_AT, nextCycle);
        FakeCommunicator core = new FakeCommunicator(List.of(
                new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(30), 42.0)));
        FakeCommunicator external = new FakeCommunicator(List.of());
        external.failNextSend = true;
        IccSynchronizer synchronizer = new IccSynchronizer(
                core,
                external,
                List.of(new IccMapping(
                        TIME_SERIES_ID,
                        EXTERNAL_TIME_SERIES_ID,
                        MappingDirection.CORE_TO_EXTERNAL)),
                Duration.ofHours(24),
                clock);

        synchronizer.run();
        synchronizer.run();

        Instant initialFrom = SYNC_AT.minus(Duration.ofHours(24));
        assertEquals(
                List.of(new ReadWindow(initialFrom, SYNC_AT), new ReadWindow(initialFrom, nextCycle)),
                core.requestedWindows);
        assertEquals(2, external.sendAttempts);
    }

    private static IccSynchronizer synchronizer(
            PegelHubClient core,
            FakeCommunicator external,
            MappingDirection direction) {
        return new IccSynchronizer(
                core,
                external,
                List.of(new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, direction)),
                Duration.ofHours(24),
                Clock.fixed(SYNC_AT, ZoneOffset.UTC));
    }

    private static class FakeCommunicator implements PegelHubClient {
        private final Collection<Measurement> measurements;
        private UUID requestedTimeSeriesId;
        private final List<ReadWindow> requestedWindows = new ArrayList<>();
        private List<Measurement> sentMeasurements = List.of();
        private boolean failNextSend;
        private int sendAttempts;

        private FakeCommunicator(Collection<Measurement> measurements) {
            this.measurements = measurements;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Instant from, Instant to) {
            this.requestedTimeSeriesId = timeSeriesId;
            this.requestedWindows.add(new ReadWindow(from, to));
            return measurements;
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
            sendAttempts++;
            if (failNextSend) {
                failNextSend = false;
                throw new RuntimeException("target unavailable");
            }
            this.sentMeasurements = measurements;
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    }

    private static class MissingTimeSeriesCommunicator implements PegelHubClient {
        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Instant from, Instant to) {
            throw new NotFoundException("missing");
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
            return Optional.empty();
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
        }

        @Override
        public void close() {
        }
    }

    private record ReadWindow(Instant from, Instant to) {
    }
}
