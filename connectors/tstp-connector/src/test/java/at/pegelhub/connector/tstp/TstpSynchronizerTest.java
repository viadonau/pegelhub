package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TstpSynchronizerTest {
    private static final UUID INBOUND_SERIES = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTBOUND_SERIES = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant OBSERVED_AT = Instant.parse("2026-06-07T10:15:30Z");

    @Test
    void processesMixedDirectionsSequentially() {
        FakeCoreClient core = new FakeCoreClient(List.of(
                new Measurement(OUTBOUND_SERIES, OBSERVED_AT, 42.0)));
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.readMeasurements = List.of(new Measurement(null, OBSERVED_AT, 42.0));

        synchronizer(core, tstp, List.of(
                mapping(INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE),
                mapping(OUTBOUND_SERIES, 22, MappingDirection.CORE_TO_EXTERNAL))).run();

        assertEquals(List.of(11, 22), tstp.catalogRequests);
        assertEquals(1, core.sent.size());
        assertEquals(INBOUND_SERIES, core.sent.getFirst().getTimeSeriesId());
        assertEquals(1, tstp.written.size());
        assertEquals(OUTBOUND_SERIES, tstp.written.getFirst().getTimeSeriesId());
        assertEquals(List.of("read:zrid-11", "write:zrid-22"), tstp.operations);
    }

    @Test
    void continuesAfterMappingFailure() {
        FakeCoreClient core = new FakeCoreClient(List.of());
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.failingStations.add(11);
        tstp.readMeasurements = List.of(new Measurement(null, OBSERVED_AT, 7.0));

        synchronizer(
                core,
                tstp,
                List.of(
                        mapping(INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE),
                        mapping(OUTBOUND_SERIES, 22, MappingDirection.EXTERNAL_TO_CORE)))
                .run();

        assertEquals(1, core.sent.size());
        assertEquals(OUTBOUND_SERIES, core.sent.getFirst().getTimeSeriesId());
    }

    @Test
    void coversElapsedTimeBetweenFixedDelayCycles() {
        Instant startedAt = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(startedAt);
        FakeCoreClient core = new FakeCoreClient(List.of());
        FakeTstpClient tstp = new FakeTstpClient();
        TstpSynchronizer synchronizer = synchronizer(
                core,
                tstp,
                List.of(
                        mapping(INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE),
                        mapping(OUTBOUND_SERIES, 22, MappingDirection.CORE_TO_EXTERNAL)),
                Duration.ofMinutes(10),
                clock);

        synchronizer.run();
        clock.advance(Duration.ofMinutes(10).plusSeconds(20));
        synchronizer.run();

        assertEquals(2, clock.readCount());
        assertEquals(
                List.of(Duration.ofMinutes(10).plusSeconds(1), Duration.ofMinutes(10).plusSeconds(21)),
                core.lookbacks);
        assertEquals(new ReadWindow(startedAt.minus(Duration.ofMinutes(10)), startedAt), tstp.readWindows.get(0));
        assertEquals(new ReadWindow(startedAt, clock.current()), tstp.readWindows.get(1));
    }

    private static TstpSynchronizer synchronizer(
            FakeCoreClient core,
            FakeTstpClient tstp,
            List<TstpMapping> mappings) {
        return new TstpSynchronizer(
                core,
                tstp,
                new TstpCatalogResolver(tstp),
                mappings,
                Duration.ofHours(24),
                Clock.fixed(OBSERVED_AT.plusSeconds(1), ZoneOffset.UTC));
    }

    private static TstpSynchronizer synchronizer(
            FakeCoreClient core,
            FakeTstpClient tstp,
            List<TstpMapping> mappings,
            Duration initialLookback,
            Clock clock) {
        return new TstpSynchronizer(
                core,
                tstp,
                new TstpCatalogResolver(tstp),
                mappings,
                initialLookback,
                clock);
    }

    private static TstpMapping mapping(
            UUID timeSeriesId,
            int stationId,
            MappingDirection direction) {
        return new TstpMapping(timeSeriesId, stationId, direction);
    }

    private static final class FakeCoreClient implements PegelHubClient {
        private final Collection<Measurement> outbound;
        private final List<Duration> lookbacks = new ArrayList<>();
        private List<Measurement> sent = List.of();

        private FakeCoreClient(Collection<Measurement> outbound) {
            this.outbound = outbound;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration lookback) {
            lookbacks.add(lookback);
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
        private final List<ReadWindow> readWindows = new ArrayList<>();
        private List<Measurement> readMeasurements = List.of();
        private List<Measurement> written = List.of();

        @Override
        public List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil) {
            operations.add("read:" + zrid);
            readWindows.add(new ReadWindow(readFrom, readUntil));
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

    private record ReadWindow(
            Instant from,
            Instant until
    ) {}

    private static final class MutableClock extends Clock {
        private Instant now;
        private int readCount;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        Instant current() {
            return now;
        }

        int readCount() {
            return readCount;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            readCount++;
            return now;
        }
    }
}
