package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IccSynchronizerTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXTERNAL_TIME_SERIES_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldSyncCoreMeasurementsToExternalCore() {
        List<Measurement> measurements = List.of(new Measurement(TIME_SERIES_ID, Instant.parse("2026-06-07T10:15:30Z"), 42.0));
        FakeCommunicator core = new FakeCommunicator(measurements);
        FakeCommunicator external = new FakeCommunicator(List.of());

        new IccSynchronizer(core, external, List.of(
                new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, MappingDirection.CORE_TO_EXTERNAL)), Duration.ofHours(24)).run();

        assertEquals(TIME_SERIES_ID, core.requestedTimeSeriesId);
        assertEquals(Duration.ofHours(24), core.requestedTimespan);
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

        new IccSynchronizer(core, external, List.of(
                new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, MappingDirection.EXTERNAL_TO_CORE)), Duration.ofHours(24)).run();

        assertEquals(EXTERNAL_TIME_SERIES_ID, external.requestedTimeSeriesId);
        assertEquals(Duration.ofHours(24), external.requestedTimespan);
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

        new IccSynchronizer(core, external, List.of(
                new IccMapping(TIME_SERIES_ID, EXTERNAL_TIME_SERIES_ID, MappingDirection.CORE_TO_EXTERNAL)), Duration.ofHours(24)).run();

        assertTrue(external.sentMeasurements.isEmpty());
    }

    private static class FakeCommunicator implements PegelHubClient {
        private final Collection<Measurement> measurements;
        private UUID requestedTimeSeriesId;
        private Duration requestedTimespan;
        private List<Measurement> sentMeasurements = List.of();

        private FakeCommunicator(Collection<Measurement> measurements) {
            this.measurements = measurements;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration timespan) {
            this.requestedTimeSeriesId = timeSeriesId;
            this.requestedTimespan = timespan;
            return measurements;
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
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
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration timespan) {
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
}
