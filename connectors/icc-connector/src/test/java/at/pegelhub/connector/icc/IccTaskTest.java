package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.internal.MockPegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IccTaskTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldSyncMeasurementsByTimeSeriesId() {
        List<Measurement> measurements = List.of(new Measurement(TIME_SERIES_ID, Instant.parse("2026-06-07T10:15:30Z"), 42.0));
        FakeCommunicator source = new FakeCommunicator(measurements);
        FakeCommunicator sink = new FakeCommunicator(List.of());

        new IccTask(source, sink, List.of(TIME_SERIES_ID), "24h").run();

        assertEquals(TIME_SERIES_ID, source.requestedTimeSeriesId);
        assertEquals("24h", source.requestedTimespan);
        assertEquals(measurements, sink.sentMeasurements);
    }

    @Test
    void shouldSkipMissingTimeSeries() {
        PegelHubCommunicator source = new MissingTimeSeriesCommunicator();
        FakeCommunicator sink = new FakeCommunicator(List.of());

        new IccTask(source, sink, List.of(TIME_SERIES_ID), "24h").run();

        assertTrue(sink.sentMeasurements.isEmpty());
    }

    private static class FakeCommunicator extends MockPegelHubCommunicator {
        private final Collection<Measurement> measurements;
        private UUID requestedTimeSeriesId;
        private String requestedTimespan;
        private List<Measurement> sentMeasurements = List.of();

        private FakeCommunicator(Collection<Measurement> measurements) {
            this.measurements = measurements;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan) {
            this.requestedTimeSeriesId = timeSeriesId;
            this.requestedTimespan = timespan;
            return measurements;
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
            this.sentMeasurements = measurements;
        }
    }

    private static class MissingTimeSeriesCommunicator extends MockPegelHubCommunicator {
        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan) {
            throw new NotFoundException("missing");
        }
    }
}
