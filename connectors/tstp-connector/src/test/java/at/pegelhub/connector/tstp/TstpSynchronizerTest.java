package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.runtime.LoadedMapping;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TstpSynchronizerTest {
    private static final UUID INBOUND_SERIES = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTBOUND_SERIES = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant OBSERVED_AT = Instant.parse("2026-06-07T10:15:30Z");

    @Test
    void processesMixedDirectionsSequentiallyAndVerifiesOutboundRoundTrip() {
        FakeCoreClient core = new FakeCoreClient(List.of(
                new Measurement(OUTBOUND_SERIES, OBSERVED_AT, 42.0)));
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.readMeasurements = List.of(new Measurement(null, OBSERVED_AT, 42.0));

        synchronizer(core, tstp, List.of(
                mapping("01-in.yaml", INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE, false),
                mapping("02-out.yaml", OUTBOUND_SERIES, 22, MappingDirection.CORE_TO_EXTERNAL, true))).run();

        assertEquals(List.of(11, 22), tstp.catalogRequests);
        assertEquals(1, core.sent.size());
        assertEquals(INBOUND_SERIES, core.sent.getFirst().getTimeSeriesId());
        assertEquals(1, tstp.written.size());
        assertEquals(OUTBOUND_SERIES, tstp.written.getFirst().getTimeSeriesId());
        assertEquals(3, tstp.operations.size());
        assertEquals(List.of("read:zrid-11", "write:zrid-22", "read:zrid-22"), tstp.operations);
    }

    @Test
    void continuesAfterMappingFailureAndThrowsCycleSummary() {
        FakeCoreClient core = new FakeCoreClient(List.of());
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.failingStations.add(11);
        tstp.readMeasurements = List.of(new Measurement(null, OBSERVED_AT, 7.0));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> synchronizer(
                core,
                tstp,
                List.of(
                        mapping("broken.yaml", INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE, false),
                        mapping("working.yaml", OUTBOUND_SERIES, 22, MappingDirection.EXTERNAL_TO_CORE, false)))
                .run());

        assertTrue(error.getMessage().contains("1 of 2"));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(1, core.sent.size());
        assertEquals(OUTBOUND_SERIES, core.sent.getFirst().getTimeSeriesId());
    }

    private static TstpSynchronizer synchronizer(
            FakeCoreClient core,
            FakeTstpClient tstp,
            List<LoadedMapping<TstpMapping>> mappings) {
        return new TstpSynchronizer(
                core,
                tstp,
                new TstpCatalogResolver(tstp),
                mappings,
                Duration.ofHours(24));
    }

    private static LoadedMapping<TstpMapping> mapping(
            String fileName,
            UUID timeSeriesId,
            int stationId,
            MappingDirection direction,
            boolean verifyRoundTrip) {
        return new LoadedMapping<>(
                fileName,
                new TstpMapping(timeSeriesId, stationId, direction, verifyRoundTrip));
    }

    private static final class FakeCoreClient implements PegelHubClient {
        private final Collection<Measurement> outbound;
        private List<Measurement> sent = List.of();

        private FakeCoreClient(Collection<Measurement> outbound) {
            this.outbound = outbound;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration lookback) {
            return outbound;
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
            return Optional.empty();
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
            sent = measurements;
        }

        @Override
        public void close() {
        }
    }

    private static final class FakeTstpClient implements TstpClient {
        private final List<Integer> catalogRequests = new ArrayList<>();
        private final List<Integer> failingStations = new ArrayList<>();
        private final List<String> operations = new ArrayList<>();
        private List<Measurement> readMeasurements = List.of();
        private List<Measurement> written = List.of();

        @Override
        public List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil) {
            operations.add("read:" + zrid);
            return readMeasurements;
        }

        @Override
        public XmlQueryResponse readCatalog(int stationId) {
            catalogRequests.add(stationId);
            if (failingStations.contains(stationId)) {
                throw new IllegalStateException("catalog unavailable");
            }
            XmlQueryTsAttribut attribute = new XmlQueryTsAttribut();
            attribute.setZrid("zrid-" + stationId);
            XmlQueryResponse response = new XmlQueryResponse();
            response.setDef(List.of(attribute));
            return response;
        }

        @Override
        public void writeMeasurements(String zrid, List<Measurement> measurements) {
            operations.add("write:" + zrid);
            written = measurements;
        }

        @Override
        public void close() {
        }
    }
}
