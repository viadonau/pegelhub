package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.HttpTstpClient;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.config.TstpServer;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TstpSynchronizerTest {
    @Test
    void importsOnlyRecordedUtcReadingsAndDoesNotSendGapOnlyPollsToCore() throws Exception {
        FakeCoreClient core = new FakeCoreClient(List.of());
        MutableClock clock = new MutableClock(Instant.parse("2026-09-16T12:00:00Z"));
        Instant wireStart = Instant.parse("2026-09-16T11:45:00Z");
        AtomicReference<String> payload = new AtomicReference<>(BinaryResponseFixture.response(List.of(
                new Measurement(null, wireStart.minusSeconds(1), 41.99),
                new Measurement(null, wireStart, 42),
                new Measurement(null, wireStart.plusSeconds(300), 4e37),
                new Measurement(null, wireStart.plusSeconds(900), 43),
                new Measurement(null, wireStart.plusSeconds(4500), 44),
                new Measurement(null, wireStart.plusSeconds(4501), 44.01)), "cm"));
        List<String> queries = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            byte[] response;
            if (query.startsWith("Cmd=Query&")) {
                response = ("<TSQ><TSATTR><ZRID>test-series</ZRID><ORT>11</ORT>"
                        + "<PARAMETER>Wasserstand</PARAMETER><EINHEIT>cm</EINHEIT>"
                        + "<HAUPTREIHE>T</HAUPTREIHE></TSATTR></TSQ>")
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                queries.add(query);
                response = payload.get().getBytes(StandardCharsets.UTF_8);
            }
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try (TstpClient client = HttpTstpClient.open(new TstpServer("127.0.0.1", server.getAddress().getPort(), "+01:00"))) {
            TstpSynchronizer sync = new TstpSynchronizer(core, client, new TstpCatalogResolver(client),
                    List.of(mapping(INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE)),
                    Duration.ofMinutes(15), clock);
            sync.run();

            assertEquals(List.of(Instant.parse("2026-09-16T10:45:00Z"), Instant.parse("2026-09-16T11:00:00Z")),
                    core.sent.stream().map(Measurement::getObservedAt).toList());
            assertEquals(List.of(42.0, 43.0), core.sent.stream().map(Measurement::getValue).toList());
            assertEquals(List.of(INBOUND_SERIES, INBOUND_SERIES),
                    core.sent.stream().map(Measurement::getTimeSeriesId).toList());
            List<Measurement> lastSend = core.sent;

            payload.set(BinaryResponseFixture.response(List.of(new Measurement(null, wireStart.plusSeconds(3000), 4e37)), "cm"));
            clock.advance(Duration.ofMinutes(15));
            sync.run();
            clock.advance(Duration.ofMinutes(15));
            sync.run();

            assertSame(lastSend, core.sent);
            assertEquals(List.of(
                    "Cmd=Get&ZRID=test-series&Von=2026-09-16T11:44:59Z&Bis=2026-09-16T13:00:01Z&WERTE=True",
                    "Cmd=Get&ZRID=test-series&Von=2026-09-16T11:59:59Z&Bis=2026-09-16T13:15:01Z&WERTE=True",
                    "Cmd=Get&ZRID=test-series&Von=2026-09-16T12:14:59Z&Bis=2026-09-16T13:30:01Z&WERTE=True"), queries);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingOrNonPositiveOverlap() {
        var core = new FakeCoreClient(List.of());
        var tstp = new FakeTstpClient();
        var catalog = new TstpCatalogResolver(tstp);

        for (Duration overlap : new Duration[]{null, Duration.ZERO, Duration.ofNanos(-1)}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new TstpSynchronizer(core, tstp, catalog, List.of(), Duration.ofHours(1), overlap));
        }
    }

    @Test
    void deliversUpstreamReadingAfterEmptySuccessfulPolls() {
        Instant start = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        List<Measurement> upstream = new ArrayList<>();
        FakeCoreClient core = new FakeCoreClient(upstream);
        FakeTstpClient tstp = new FakeTstpClient();
        TstpSynchronizer sync = synchronizer(core, tstp,
                List.of(mapping(OUTBOUND_SERIES, 22, MappingDirection.CORE_TO_EXTERNAL)),
                Duration.ofMinutes(15), clock);
        sync.run();
        clock.advance(Duration.ofMinutes(15));
        sync.run();
        // IEC and then ICC deliver an older observation into this Core.
        upstream.add(new Measurement(OUTBOUND_SERIES, start.minusSeconds(1), 42));
        clock.advance(Duration.ofMinutes(15));
        sync.run();
        assertEquals(1, tstp.written.size());
        assertEquals(start.minusSeconds(1), tstp.written.getFirst().getObservedAt());
    }
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
    void doesNotCacheCatalogEntriesWithoutAZrid() {
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.stationsWithoutZrid.add(11);
        TstpCatalogResolver resolver = new TstpCatalogResolver(tstp);

        assertThrows(IllegalStateException.class,
                () -> resolver.resolveZrid(11, TstpParameter.WATER_LEVEL, "cm"));

        tstp.stationsWithoutZrid.clear();
        assertEquals("zrid-11", resolver.resolveZrid(11, TstpParameter.WATER_LEVEL, "cm"));
        assertEquals(List.of(11, 11), tstp.catalogRequests);
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
                List.of(
                        new ReadWindow(startedAt.minus(Duration.ofMinutes(70)), startedAt),
                        new ReadWindow(startedAt.minus(Duration.ofHours(1)), clock.current())),
                core.readWindows);
        assertEquals(new ReadWindow(startedAt.minus(Duration.ofMinutes(70)), startedAt), tstp.readWindows.get(0));
        assertEquals(new ReadWindow(startedAt.minus(Duration.ofHours(1)), clock.current()), tstp.readWindows.get(1));
    }

    @Test
    void retainsTheInitialStartBoundaryAfterFailure() {
        Instant startedAt = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(startedAt);
        FakeCoreClient core = new FakeCoreClient(List.of());
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.failingStations.add(11);
        TstpSynchronizer synchronizer = synchronizer(
                core,
                tstp,
                List.of(mapping(INBOUND_SERIES, 11, MappingDirection.EXTERNAL_TO_CORE)),
                Duration.ofMinutes(10),
                clock);

        synchronizer.run();
        clock.advance(Duration.ofMinutes(12));
        tstp.failingStations.clear();
        synchronizer.run();

        assertEquals(
                List.of(new ReadWindow(startedAt.minus(Duration.ofMinutes(70)), clock.current())),
                tstp.readWindows);
    }

    @ParameterizedTest
    @CsvSource({"CORE_TO_EXTERNAL,5", "CORE_TO_EXTERNAL,15",
            "EXTERNAL_TO_CORE,5", "EXTERNAL_TO_CORE,15"})
    void replaysLateReadingsWithCompleteNeighbors(MappingDirection direction, int minutes) {
        Instant start = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        List<Measurement> data = new ArrayList<>();
        FakeCoreClient core = new FakeCoreClient(data);
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.readMeasurements = data;
        TstpSynchronizer sync = synchronizer(core, tstp,
                List.of(mapping(OUTBOUND_SERIES, 22, direction)), Duration.ofMinutes(minutes), clock);
        sync.run();
        data.add(new Measurement(OUTBOUND_SERIES, start.minusSeconds(180), 1));
        data.add(new Measurement(OUTBOUND_SERIES, start.minusSeconds(60), 3));
        clock.advance(Duration.ofMinutes(minutes));
        sync.run();
        assertEquals(2, delivered(core, tstp, direction).size());
        data.add(1, new Measurement(OUTBOUND_SERIES, start.minusSeconds(120), 2));
        clock.advance(Duration.ofMinutes(minutes));
        sync.run();
        assertEquals(List.of(1.0, 2.0, 3.0),
                delivered(core, tstp, direction).stream().map(Measurement::getValue).toList());
    }

    @ParameterizedTest
    @CsvSource({"CORE_TO_EXTERNAL,true", "CORE_TO_EXTERNAL,false",
            "EXTERNAL_TO_CORE,true", "EXTERNAL_TO_CORE,false"})
    void retainsWindowAfterReadOrWriteFailure(MappingDirection direction, boolean readFailure) {
        Instant start = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        List<Measurement> data = List.of(new Measurement(OUTBOUND_SERIES, start.minusSeconds(4200), 42));
        FakeCoreClient core = new FakeCoreClient(data);
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.readMeasurements = data;
        core.failNextRead = readFailure && direction == MappingDirection.CORE_TO_EXTERNAL;
        tstp.failNextRead = readFailure && direction == MappingDirection.EXTERNAL_TO_CORE;
        core.failNextSend = !readFailure && direction == MappingDirection.EXTERNAL_TO_CORE;
        tstp.failNextWrite = !readFailure && direction == MappingDirection.CORE_TO_EXTERNAL;
        TstpSynchronizer sync = synchronizer(core, tstp,
                List.of(mapping(OUTBOUND_SERIES, 22, direction)), Duration.ofMinutes(15), clock);
        sync.run();
        clock.advance(Duration.ofMinutes(15));
        sync.run();
        assertEquals(42, delivered(core, tstp, direction).getFirst().getValue());
        List<ReadWindow> reads = direction == MappingDirection.CORE_TO_EXTERNAL ? core.readWindows : tstp.readWindows;
        assertEquals(start.minusSeconds(4500), reads.getFirst().from());
        assertEquals(reads.getFirst().from(), reads.get(1).from());
        clock.advance(Duration.ofMinutes(15));
        sync.run();
        assertEquals(start.minusSeconds(2700), reads.get(2).from());
    }

    @ParameterizedTest
    @EnumSource(MappingDirection.class)
    void respectsWholeSecondBoundariesRestartAndFiniteOverlap(MappingDirection direction) {
        Instant start = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(start.plusMillis(500));
        List<Measurement> data = new ArrayList<>(List.of(
                new Measurement(OUTBOUND_SERIES, start.minusSeconds(4500), 1),
                new Measurement(OUTBOUND_SERIES, start, 2)));
        FakeCoreClient core = new FakeCoreClient(data);
        FakeTstpClient tstp = new FakeTstpClient();
        tstp.readMeasurements = data;
        List<TstpMapping> mappings = List.of(mapping(OUTBOUND_SERIES, 22, direction));
        TstpSynchronizer sync = synchronizer(core, tstp, mappings, Duration.ofMinutes(15), clock);
        sync.run();
        assertEquals(List.of(1.0), delivered(core, tstp, direction).stream().map(Measurement::getValue).toList());
        data.add(new Measurement(OUTBOUND_SERIES, start.minusSeconds(3601), 3));
        data.add(new Measurement(OUTBOUND_SERIES, start.minusSeconds(3600), 4));
        clock.advance(Duration.ofMinutes(15));
        sync.run();
        assertEquals(List.of(2.0, 4.0), delivered(core, tstp, direction).stream()
                .sorted(java.util.Comparator.comparing(Measurement::getValue))
                .map(Measurement::getValue).toList());
        synchronizer(core, tstp, mappings, Duration.ofMinutes(15), clock).run();
        List<ReadWindow> reads = direction == MappingDirection.CORE_TO_EXTERNAL ? core.readWindows : tstp.readWindows;
        assertEquals(new ReadWindow(start.minusSeconds(3600), start.plusSeconds(900)), reads.get(2));
    }

    @Test
    void customOverlapNeverRegressesOnClockRollback() {
        Instant start = Instant.parse("2026-06-07T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        FakeCoreClient core = new FakeCoreClient(List.of());
        FakeTstpClient tstp = new FakeTstpClient();
        TstpSynchronizer sync = new TstpSynchronizer(core, tstp, new TstpCatalogResolver(tstp),
                List.of(mapping(OUTBOUND_SERIES, 22, MappingDirection.CORE_TO_EXTERNAL)),
                Duration.ofMinutes(5), Duration.ofMinutes(10), clock);
        sync.run();
        clock.advance(Duration.ofHours(-2));
        sync.run();
        clock.advance(Duration.ofHours(2).plusMinutes(5));
        sync.run();
        assertEquals(List.of(new ReadWindow(start.minusSeconds(900), start),
                new ReadWindow(start.minusSeconds(600), start.plusSeconds(300))), core.readWindows);
    }

    private static List<Measurement> delivered(
            FakeCoreClient core, FakeTstpClient tstp, MappingDirection direction) {
        return direction == MappingDirection.CORE_TO_EXTERNAL ? tstp.written : core.sent;
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
        return new TstpMapping(timeSeriesId, stationId, direction, TstpParameter.WATER_LEVEL, "cm");
    }

    private static final class FakeCoreClient implements PegelHubClient {
        private final Collection<Measurement> outbound;
        private final List<ReadWindow> readWindows = new ArrayList<>();
        private List<Measurement> sent = List.of();
        private boolean failNextRead;
        private boolean failNextSend;

        private FakeCoreClient(Collection<Measurement> outbound) {
            this.outbound = outbound;
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(
                UUID timeSeriesId, Instant from, Instant to, MeasurementRepresentation representation) {
            assertEquals(MeasurementRepresentation.CANONICAL, representation);
            readWindows.add(new ReadWindow(from, to));
            if (failNextRead) {
                failNextRead = false;
                throw new IllegalStateException("Core read unavailable");
            }
            return outbound.stream()
                    .filter(m -> !m.getObservedAt().isBefore(from) && m.getObservedAt().isBefore(to))
                    .toList();
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(
                UUID timeSeriesId, MeasurementRepresentation representation) {
            assertEquals(MeasurementRepresentation.CANONICAL, representation);
            return Optional.empty();
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
            if (failNextSend) {
                failNextSend = false;
                throw new IllegalStateException("Core write unavailable");
            }
            sent = measurements;
        }

        @Override
        public void close() {
        }
    }

    private static final class FakeTstpClient implements TstpClient {
        private final List<Integer> catalogRequests = new ArrayList<>();
        private final List<Integer> failingStations = new ArrayList<>();
        private final List<Integer> stationsWithoutZrid = new ArrayList<>();
        private final List<String> operations = new ArrayList<>();
        private final List<ReadWindow> readWindows = new ArrayList<>();
        private List<Measurement> readMeasurements = List.of();
        private List<Measurement> written = List.of();
        private boolean failNextRead;
        private boolean failNextWrite;

        @Override
        public List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil, String unit) {
            operations.add("read:" + zrid);
            readWindows.add(new ReadWindow(readFrom, readUntil));
            if (failNextRead) {
                failNextRead = false;
                throw new IllegalStateException("TSTP read unavailable");
            }
            return readMeasurements.stream()
                    .filter(m -> !m.getObservedAt().isBefore(readFrom) && m.getObservedAt().isBefore(readUntil))
                    .toList();
        }

        @Override
        public XmlQueryResponse readCatalog(int stationId, TstpParameter parameter) {
            catalogRequests.add(stationId);
            if (failingStations.contains(stationId)) {
                throw new IllegalStateException("catalog unavailable");
            }
            XmlQueryTsAttribut attribute = new XmlQueryTsAttribut();
            attribute.setOrt(Integer.toString(stationId));
            attribute.setParameter(parameter.value());
            attribute.setEinheit(parameter.defaultUnit());
            attribute.setHauptReihe("T");
            if (!stationsWithoutZrid.contains(stationId)) {
                attribute.setZrid("zrid-" + stationId);
            }
            XmlQueryResponse response = new XmlQueryResponse();
            response.setDef(List.of(attribute));
            return response;
        }

        @Override
        public void writeMeasurements(String zrid, List<Measurement> measurements, String unit) {
            operations.add("write:" + zrid);
            if (failNextWrite) {
                failNextWrite = false;
                throw new IllegalStateException("TSTP write unavailable");
            }
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
