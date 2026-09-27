package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementIntervalStatistics;
import at.pegelhub.lib.model.MeasurementRepresentation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TstpIntervalMeanExportTest {
    private static final UUID SERIES = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant START = Instant.parse("2026-07-15T22:30:00Z");

    @Test
    void publishesEndLabeledMeansToTheExistingMappedSeries() {
        FakeCore core = new FakeCore();
        core.means.put(START, 0.0);
        core.means.put(START.plusSeconds(1_800), 12.5);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-16T00:16:00Z"));
        FakeTstp tstp = new FakeTstp();
        TstpSynchronizer synchronizer = synchronizer(core, tstp, clock, meanExport("15m"));

        synchronizer.run();

        assertEquals(List.of(expectedRead(START, START.plusSeconds(2_700), "15m")), core.reads);
        assertEquals(List.of(77), tstp.catalogRequests);
        assertEquals("zrid-77", tstp.writes.getFirst().zrid());
        assertEquals("cm", tstp.writes.getFirst().unit());
        assertEquals(List.of(START.plusSeconds(900), START.plusSeconds(1_800), START.plusSeconds(2_700)),
                timestamps(tstp.writes.getFirst()));
        assertEquals(Arrays.asList(0.0, null, 12.5), values(tstp.writes.getFirst()));

        synchronizer.run();
        assertEquals(1, core.reads.size());
        assertEquals(1, tstp.writes.size());

        core.means.put(START.plusSeconds(1_800), 13.5);
        clock.advance(Duration.ofMinutes(15));
        synchronizer.run();

        assertEquals(expectedRead(START.plusSeconds(1_800), START.plusSeconds(3_600), "15m"),
                core.reads.get(1));
        assertEquals(Arrays.asList(13.5, null), values(tstp.writes.get(1)));
    }

    @Test
    void publishesSuccessiveEqualMeansEvenWithoutNewObservations() {
        FakeCore core = new FakeCore();
        for (int index = 0; index < 3; index++) {
            Instant start = START.plusSeconds(index * 900L);
            core.means.put(start, 18.0);
            core.observationCounts.put(start, index == 0 ? 1L : 0L);
        }
        FakeTstp tstp = new FakeTstp();

        synchronizer(core, tstp, new MutableClock(Instant.parse("2026-07-16T00:16:00Z")),
                meanExport("15m")).run();

        assertEquals(List.of(18.0, 18.0, 18.0), values(tstp.writes.getFirst()));
    }

    @Test
    void failuresDoNotAdvanceTheReplayStart() {
        FakeCore core = new FakeCore();
        core.means.put(START, 1.0);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-16T00:16:00Z"));
        FakeTstp tstp = new FakeTstp();
        TstpSynchronizer synchronizer = synchronizer(core, tstp, clock, meanExport("15m"));
        synchronizer.run();

        clock.advance(Duration.ofMinutes(15));
        core.failNextRead = true;
        synchronizer.run();
        clock.advance(Duration.ofMinutes(15));
        tstp.failNextWrite = true;
        synchronizer.run();
        clock.advance(Duration.ofMinutes(15));
        synchronizer.run();

        assertEquals(START.plusSeconds(1_800), core.reads.get(1).from());
        assertEquals(core.reads.get(1).from(), core.reads.get(2).from());
        assertEquals(core.reads.get(1).from(), core.reads.get(3).from());
    }

    @Test
    void doesNotAdvanceWhenCoreHasNotClosedEveryRequestedInterval() {
        FakeCore core = new FakeCore();
        core.laggingResponse = true;
        FakeTstp tstp = new FakeTstp();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-16T00:16:00Z"));
        TstpSynchronizer synchronizer = synchronizer(core, tstp, clock, meanExport("15m"));

        synchronizer.run();
        core.laggingResponse = false;
        clock.advance(Duration.ofMinutes(15));
        synchronizer.run();

        assertEquals(core.reads.getFirst().from(), core.reads.get(1).from());
        assertEquals(1, tstp.writes.size());
    }

    @Test
    void dailyMezWindowsRemainFixedAcrossTheDstChange() {
        FakeCore core = new FakeCore();
        FakeTstp tstp = new FakeTstp();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-30T00:01:00Z"));
        TstpMapping mapping = mapping(meanExport("1d"));

        new TstpSynchronizer(core, tstp, new TstpCatalogResolver(tstp), List.of(mapping),
                Duration.ofMinutes(15), Duration.ofMinutes(15), clock).run();

        assertEquals(expectedRead(Instant.parse("2026-03-28T23:00:00Z"),
                Instant.parse("2026-03-29T23:00:00Z"), "1d"), core.reads.getFirst());
        assertEquals(List.of(Instant.parse("2026-03-29T23:00:00Z")), timestamps(tstp.writes.getFirst()));
    }

    @Test
    void catchesUpInBoundedBatchesWithoutSkippingTheOverlap() {
        FakeCore core = new FakeCore();
        FakeTstp tstp = new FakeTstp();
        Instant end = START.plusSeconds(1_001L * 900);
        var clock = new MutableClock(end.plusSeconds(3_660));
        var synchronizer = new TstpSynchronizer(core, tstp, new TstpCatalogResolver(tstp),
                List.of(mapping(meanExport("15m"))), Duration.ofMinutes(1_000L * 15),
                Duration.ofMinutes(15), clock);

        synchronizer.run();
        synchronizer.run();
        synchronizer.run();

        assertEquals(List.of(
                expectedRead(START, START.plusSeconds(1_000L * 900), "15m"),
                expectedRead(START.plusSeconds(999L * 900), end, "15m")), core.reads);
        assertEquals(List.of(1_000, 2), tstp.writes.stream().map(write -> write.measurements().size()).toList());
        assertEquals(timestamps(tstp.writes.getFirst()).getLast(), timestamps(tstp.writes.getLast()).getFirst());
        assertEquals(end, timestamps(tstp.writes.getLast()).getLast());
    }

    private static TstpSynchronizer synchronizer(
            FakeCore core,
            FakeTstp tstp,
            Clock clock,
            TstpMeanExport export) {
        return new TstpSynchronizer(core, tstp, new TstpCatalogResolver(tstp), List.of(mapping(export)),
                Duration.ofMinutes(30), Duration.ofMinutes(15), clock);
    }

    private static TstpMapping mapping(TstpMeanExport export) {
        return new TstpMapping(SERIES, 77, MappingDirection.CORE_TO_EXTERNAL,
                TstpParameter.WATER_LEVEL, "cm", export);
    }

    private static TstpMeanExport meanExport(String interval) {
        return new TstpMeanExport(interval, "+01:00", "1h");
    }

    private static List<Instant> timestamps(Write write) {
        return write.measurements().stream().map(Measurement::getObservedAt).toList();
    }

    private static List<Double> values(Write write) {
        return write.measurements().stream().map(Measurement::getValue).toList();
    }

    private static IntervalRead expectedRead(Instant from, Instant to, String interval) {
        return new IntervalRead(SERIES, from, to, interval, "+01:00", true,
                MeasurementRepresentation.CANONICAL);
    }

    private record IntervalRead(
            UUID series,
            Instant from,
            Instant to,
            String interval,
            String timeBasis,
            boolean closedOnly,
            MeasurementRepresentation representation) {}

    private record Write(String zrid, List<Measurement> measurements, String unit) {}

    private static final class FakeCore implements PegelHubClient {
        private final Map<Instant, Double> means = new HashMap<>();
        private final Map<Instant, Long> observationCounts = new HashMap<>();
        private final List<IntervalRead> reads = new ArrayList<>();
        private boolean failNextRead;
        private boolean laggingResponse;

        @Override
        public MeasurementIntervalStatistics getMeasurementIntervals(
                UUID id,
                Instant from,
                Instant to,
                String interval,
                String timeBasis,
                boolean closedOnly,
                MeasurementRepresentation representation) {
            reads.add(new IntervalRead(id, from, to, interval, timeBasis, closedOnly, representation));
            if (failNextRead) {
                failNextRead = false;
                throw new IllegalStateException("Core unavailable");
            }

            long width = switch (interval) {
                case "15m" -> 900;
                case "1h" -> 3_600;
                default -> 86_400;
            };
            List<MeasurementIntervalStatistics.Interval> results = new ArrayList<>();
            for (Instant start = from; start.isBefore(to); start = start.plusSeconds(width)) {
                Double mean = means.get(start);
                long count = mean == null ? 0 : observationCounts.getOrDefault(start, 1L);
                results.add(new MeasurementIntervalStatistics.Interval(
                        start,
                        start.plusSeconds(width),
                        mean,
                        count,
                        mean == null ? 0 : width * 1_000_000_000L,
                        contributingObservation(mean, count, from, start),
                        "closed",
                        mean == null ? "absent" : "full"));
            }
            Instant computedAt = to;
            if (laggingResponse) {
                results.removeLast();
                computedAt = to.minusSeconds(width);
            }
            return new MeasurementIntervalStatistics(id, from, to, interval, timeBasis, closedOnly,
                    representation.value(), "cm", "time-weighted-step", computedAt, results);
        }

        private static Instant contributingObservation(Double mean, long count, Instant from, Instant start) {
            if (mean == null) {
                return null;
            }
            return count == 0 ? from.minusSeconds(1) : start.plusSeconds(1);
        }

        @Override
        public Collection<Measurement> getMeasurementsOfTimeSeries(
                UUID id,
                Instant from,
                Instant to,
                MeasurementRepresentation representation) {
            throw new AssertionError("interval export requested raw measurements");
        }

        @Override
        public Optional<Measurement> getLatestMeasurementOfTimeSeries(
                UUID id,
                MeasurementRepresentation representation) {
            throw new AssertionError("unexpected latest measurement read");
        }

        @Override
        public void sendMeasurements(List<Measurement> measurements) {
            throw new AssertionError("unexpected Core write");
        }

        @Override
        public void close() {}
    }

    private static final class FakeTstp implements TstpClient {
        private final List<Integer> catalogRequests = new ArrayList<>();
        private final List<Write> writes = new ArrayList<>();
        private boolean failNextWrite;

        @Override
        public List<Measurement> readMeasurements(
                String zrid,
                Instant readFrom,
                Instant readUntil,
                String unit) {
            throw new AssertionError("interval export requested TSTP measurements");
        }

        @Override
        public XmlQueryResponse readCatalog(int stationId, TstpParameter parameter) {
            catalogRequests.add(stationId);
            XmlQueryTsAttribut entry = new XmlQueryTsAttribut();
            entry.setZrid("zrid-" + stationId);
            entry.setOrt(Integer.toString(stationId));
            entry.setParameter(parameter.value());
            entry.setEinheit("cm");
            entry.setHauptReihe("T");
            XmlQueryResponse response = new XmlQueryResponse();
            response.setDef(List.of(entry));
            return response;
        }

        @Override
        public void writeMeasurements(String zrid, List<Measurement> measurements, String unit) {
            if (failNextWrite) {
                failNextWrite = false;
                throw new IllegalStateException("TSTP unavailable");
            }
            writes.add(new Write(zrid, List.copyOf(measurements), unit));
        }

        @Override
        public void close() {}
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
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
            return now;
        }
    }
}
