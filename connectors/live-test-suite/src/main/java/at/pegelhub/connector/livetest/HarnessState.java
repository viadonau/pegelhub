package at.pegelhub.connector.livetest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class HarnessState {
    final Instant startedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    final MeasurementRecord tstpReaderMeasurement =
            new MeasurementRecord(SuiteConstants.TSTP_READER_TS, startedAt.minusSeconds(5), 664.7);
    final MeasurementRecord tstpWriterFirst =
            new MeasurementRecord(SuiteConstants.TSTP_WRITER_TS, startedAt.minusSeconds(45), 301.1);
    final MeasurementRecord tstpWriterSecond =
            new MeasurementRecord(SuiteConstants.TSTP_WRITER_TS, startedAt.minusSeconds(15), 302.2);
    final MeasurementRecord iecCoreToExternalMeasurement =
            new MeasurementRecord(SuiteConstants.IEC_CORE_TO_EXTERNAL_TS, startedAt.minusSeconds(10), 512.25);
    final MeasurementRecord iccLocalSourceMeasurement =
            new MeasurementRecord(SuiteConstants.ICC_LOCAL_SOURCE_TS, startedAt.minusSeconds(30), 51.2);
    final MeasurementRecord iccExternalSourceMeasurement =
            new MeasurementRecord(SuiteConstants.ICC_EXTERNAL_SOURCE_TS, startedAt.minusSeconds(15), 61.2);
    final FakeCoreState localCore = new FakeCoreState("local");
    final FakeCoreState externalCore = new FakeCoreState("external");
    final List<TokenRequest> tokenRequests = new CopyOnWriteArrayList<>();
    final List<TstpRequest> tstpRequests = new CopyOnWriteArrayList<>();
    final List<IecCapture> iecCaptures = new CopyOnWriteArrayList<>();

    List<FakeCoreState> cores() {
        return List.of(localCore, externalCore);
    }
}

final class FakeCoreState {
    private final String name;
    private final Map<UUID, List<MeasurementRecord>> seeded = new ConcurrentHashMap<>();
    private final List<MeasurementRecord> writes = new CopyOnWriteArrayList<>();
    private final List<CoreRequest> requests = new CopyOnWriteArrayList<>();

    FakeCoreState(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    void seed(MeasurementRecord measurement) {
        seeded.computeIfAbsent(measurement.timeSeriesId(), ignored -> Collections.synchronizedList(new ArrayList<>()))
                .add(measurement);
    }

    List<MeasurementRecord> seeded(UUID timeSeriesId) {
        return List.copyOf(seeded.getOrDefault(timeSeriesId, List.of()));
    }

    void write(MeasurementRecord measurement) {
        writes.add(measurement);
    }

    List<MeasurementRecord> writes() {
        return List.copyOf(writes);
    }

    void request(CoreRequest request) {
        requests.add(request);
    }

    List<CoreRequest> requests() {
        return List.copyOf(requests);
    }
}
