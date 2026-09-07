package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

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
    @Test
    void deliversReadingInsertedAfterAnEmptySuccessfulPoll() {
        Instant firstPoll = Instant.parse("2026-06-07T11:00:00Z");
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(firstPoll, firstPoll.plusSeconds(900));
        List<Measurement> source = new ArrayList<>();
        FakeCommunicator core = new FakeCommunicator(source);
        FakeCommunicator external = new FakeCommunicator(List.of());
        IccSynchronizer sync = new IccSynchronizer(core, external,
                List.of(new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID,
                        MappingDirection.CORE_TO_EXTERNAL)), Duration.ofMinutes(15), clock);

        sync.run();
        source.add(new Measurement(TIME_SERIES_ID, firstPoll.minusSeconds(30), 42));
        sync.run();

        assertEquals(1, external.sentMeasurements.size());
        assertEquals(firstPoll.minusSeconds(30), external.sentMeasurements.getFirst().getObservedAt());
    }
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
        assertEquals(List.of(new ReadWindow(SYNC_AT.minus(Duration.ofHours(25)), SYNC_AT)), core.requestedWindows);
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
        assertEquals(List.of(new ReadWindow(SYNC_AT.minus(Duration.ofHours(25)), SYNC_AT)), external.requestedWindows);
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

        Instant initialFrom = SYNC_AT.minus(Duration.ofHours(25));
        assertEquals(
                List.of(new ReadWindow(initialFrom, SYNC_AT), new ReadWindow(initialFrom, nextCycle)),
                core.requestedWindows);
        assertEquals(2, external.sendAttempts);
    }

    @ParameterizedTest
    @CsvSource({"CORE_TO_EXTERNAL,5", "CORE_TO_EXTERNAL,15",
            "EXTERNAL_TO_CORE,5", "EXTERNAL_TO_CORE,15"})
    void replaysLateReadingWithItsPreviouslyDeliveredNeighbors(MappingDirection direction, int minutes) {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(SYNC_AT, SYNC_AT.plusSeconds(minutes * 60L));
        List<Measurement> data = new ArrayList<>(List.of(
                new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(180), 1),
                new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(60), 3)));
        FakeCommunicator source = new FakeCommunicator(data);
        FakeCommunicator target = new FakeCommunicator(List.of());
        IccSynchronizer sync = directedSynchronizer(source, target, direction, minutes, clock);
        sync.run();
        assertEquals(2, target.sentMeasurements.size());
        data.add(1, new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(120), 2));
        sync.run();
        assertEquals(List.of(1.0, 2.0, 3.0),
                target.sentMeasurements.stream().map(Measurement::getValue).toList());
        assertEquals(SYNC_AT.minusSeconds(3600), source.requestedWindows.get(1).from());
    }

    @ParameterizedTest
    @CsvSource({"CORE_TO_EXTERNAL,true", "CORE_TO_EXTERNAL,false",
            "EXTERNAL_TO_CORE,true", "EXTERNAL_TO_CORE,false"})
    void retainsWindowAcrossReadOrWriteFailure(MappingDirection direction, boolean readFailure) {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(SYNC_AT, SYNC_AT.plusSeconds(900), SYNC_AT.plusSeconds(1800));
        FakeCommunicator source = new FakeCommunicator(List.of(
                new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(4200), 42)));
        FakeCommunicator target = new FakeCommunicator(List.of());
        source.failNextRead = readFailure;
        target.failNextSend = !readFailure;
        IccSynchronizer sync = directedSynchronizer(source, target, direction, 15, clock);
        sync.run();
        sync.run();
        assertEquals(1, target.sentMeasurements.size());
        assertEquals(source.requestedWindows.getFirst().from(), source.requestedWindows.get(1).from());
        sync.run();
        assertEquals(SYNC_AT.minusSeconds(2700), source.requestedWindows.get(2).from());
    }

    @ParameterizedTest
    @EnumSource(MappingDirection.class)
    void honorsBoundariesRestartAndFiniteOverlap(MappingDirection direction) {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(SYNC_AT, SYNC_AT.plusSeconds(900), SYNC_AT.plusSeconds(900));
        List<Measurement> data = new ArrayList<>(List.of(
                new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(4500), 1),
                new Measurement(TIME_SERIES_ID, SYNC_AT, 2)));
        FakeCommunicator source = new FakeCommunicator(data);
        FakeCommunicator target = new FakeCommunicator(List.of());
        IccSynchronizer sync = directedSynchronizer(source, target, direction, 15, clock);
        sync.run();
        assertEquals(List.of(1.0), target.sentMeasurements.stream().map(Measurement::getValue).toList());
        data.add(new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(3601), 3));
        data.add(new Measurement(TIME_SERIES_ID, SYNC_AT.minusSeconds(3600), 4));
        sync.run();
        assertEquals(List.of(2.0, 4.0), target.sentMeasurements.stream().map(Measurement::getValue).toList());
        directedSynchronizer(source, target, direction, 15, clock).run();
        assertEquals(new ReadWindow(SYNC_AT.minusSeconds(3600), SYNC_AT.plusSeconds(900)),
                source.requestedWindows.get(2));
    }

    @Test
    void customOverlapDoesNotRegressWhenClockMovesBackwards() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(SYNC_AT, SYNC_AT.minusSeconds(7200), SYNC_AT.plusSeconds(300));
        FakeCommunicator source = new FakeCommunicator(List.of());
        IccSynchronizer sync = new IccSynchronizer(source, new FakeCommunicator(List.of()),
                List.of(new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID,
                        MappingDirection.CORE_TO_EXTERNAL)), Duration.ofMinutes(5), Duration.ofMinutes(10), clock);
        sync.run();
        sync.run();
        sync.run();
        assertEquals(List.of(new ReadWindow(SYNC_AT.minusSeconds(900), SYNC_AT),
                new ReadWindow(SYNC_AT.minusSeconds(600), SYNC_AT.plusSeconds(300))), source.requestedWindows);
    }

    @Test
    void failedMappingDoesNotPreventLaterMapping() {
        PegelHubClient source = mock(PegelHubClient.class);
        Instant from = SYNC_AT.minusSeconds(4500);
        when(source.getMeasurementsOfTimeSeries(TIME_SERIES_ID, from, SYNC_AT))
                .thenThrow(new IllegalStateException("source unavailable"));
        when(source.getMeasurementsOfTimeSeries(EXTERNAL_TIME_SERIES_ID, from, SYNC_AT))
                .thenReturn(List.of(new Measurement(EXTERNAL_TIME_SERIES_ID, SYNC_AT.minusSeconds(1), 7)));
        FakeCommunicator target = new FakeCommunicator(List.of());
        new IccSynchronizer(source, target, List.of(
                new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, MappingDirection.CORE_TO_EXTERNAL),
                new IccMapping(EXTERNAL_TIME_SERIES_ID, TIME_SERIES_ID, MappingDirection.CORE_TO_EXTERNAL)),
                Duration.ofMinutes(15), Clock.fixed(SYNC_AT, ZoneOffset.UTC)).run();
        assertEquals(7, target.sentMeasurements.getFirst().getValue());
    }

    private static IccSynchronizer directedSynchronizer(
            FakeCommunicator source, FakeCommunicator target, MappingDirection direction, int minutes, Clock clock) {
        return new IccSynchronizer(
                direction == MappingDirection.CORE_TO_EXTERNAL ? source : target,
                direction == MappingDirection.CORE_TO_EXTERNAL ? target : source,
                List.of(new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, direction)),
                Duration.ofMinutes(minutes), clock);
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
        private boolean failNextRead;
        private int sendAttempts;

        private FakeCommunicator(Collection<Measurement> measurements) {
            this.measurements = measurements;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Instant from, Instant to) {
            this.requestedTimeSeriesId = timeSeriesId;
            this.requestedWindows.add(new ReadWindow(from, to));
            if (failNextRead) {
                failNextRead = false;
                throw new IllegalStateException("source unavailable");
            }
            return measurements.stream()
                    .filter(m -> !m.getObservedAt().isBefore(from) && m.getObservedAt().isBefore(to))
                    .toList();
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
