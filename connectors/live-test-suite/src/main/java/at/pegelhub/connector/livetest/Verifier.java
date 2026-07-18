package at.pegelhub.connector.livetest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class Verifier {
    private static final double TOLERANCE = 0.02;

    private Verifier() {
    }

    static VerificationResult verify(HarnessState state, Set<Scenario> scenarios) {
        List<String> failures = new ArrayList<>();
        if (Duration.between(state.startedAt, Instant.now()).compareTo(Duration.ofSeconds(5)) < 0) {
            failures.add("waiting for the initial connector cycle");
        }

        if (scenarios.contains(Scenario.FTP)) {
            verifyFtp(state, failures);
        }
        if (scenarios.contains(Scenario.TSTP)) {
            verifyTstp(state, failures);
        }
        if (scenarios.contains(Scenario.IEC)) {
            verifyIec(state, failures);
        }
        if (scenarios.contains(Scenario.ICC)) {
            verifyIcc(state, failures);
        }
        verifyUnexpectedWrites(state, scenarios, failures);

        if (failures.isEmpty()) {
            return new VerificationResult(true, "Live connector suite passed for " + scenarios + "\n" + summary(state));
        }
        return new VerificationResult(false, String.join("\n", failures) + "\n\n" + summary(state));
    }

    private static void verifyFtp(HarnessState state, List<String> failures) {
        requireToken(state, "ftp-asc", failures);
        requireToken(state, "ftp-zrxp", failures);
        requireWriteCount(state.localCore, SuiteConstants.FTP_ASC_TS, 1, failures);
        requireWriteCount(state.localCore, SuiteConstants.FTP_ZRXP_TS, 1, failures);
        requireMeasurement(state.localCore, SuiteConstants.FTP_ASC_MEASUREMENT, failures);
        requireMeasurement(state.localCore, SuiteConstants.FTP_ZRXP_MEASUREMENT, failures);
        rejectValue(state.localCore, SuiteConstants.FTP_ZRXP_TS, 118.8, failures);
        rejectValue(state.localCore, SuiteConstants.FTP_ZRXP_TS, 999.0, failures);
    }

    private static void verifyTstp(HarnessState state, List<String> failures) {
        requireToken(state, "tstp-reader", failures);
        requireToken(state, "tstp-writer", failures);
        requireTstpCommand(state, "Query", SuiteConstants.TSTP_READER_ZRID, SuiteConstants.TSTP_READER_STATION_ID, failures);
        requireTstpCommand(state, "Query", SuiteConstants.TSTP_WRITER_ZRID, SuiteConstants.TSTP_WRITER_STATION_ID, failures);
        if (state.tstpRequests.stream().noneMatch(request ->
                "Get".equalsIgnoreCase(request.command()) && SuiteConstants.TSTP_READER_ZRID.equals(request.zrid()))) {
            failures.add("TSTP reader did not request Get for " + SuiteConstants.TSTP_READER_ZRID);
        }
        requireMeasurement(state.localCore, state.tstpReaderMeasurement, failures);
        List<TstpRequest> puts = state.tstpRequests.stream()
                .filter(request -> "PUT".equalsIgnoreCase(request.command()))
                .filter(request -> SuiteConstants.TSTP_WRITER_ZRID.equals(request.zrid()))
                .toList();
        if (puts.isEmpty()) {
            failures.add("TSTP writer did not PUT to " + SuiteConstants.TSTP_WRITER_ZRID);
        } else if (puts.stream().noneMatch(request -> hasExpectedTstpPutMeasurements(state, request))) {
            failures.add("TSTP writer PUT did not contain the expected sorted measurements");
        }
        requireCoreQuery(state.localCore, SuiteConstants.TSTP_WRITER_TS, "last=61s", failures);
    }

    private static void verifyIec(HarnessState state, List<String> failures) {
        requireToken(state, "iec", failures);
        requireMeasurement(state.localCore, new MeasurementRecord(
                SuiteConstants.IEC_EXTERNAL_TO_CORE_TS,
                null,
                42.5), failures);
        rejectValue(state.localCore, SuiteConstants.IEC_EXTERNAL_TO_CORE_TS, 999.5, failures);
        if (state.iecCaptures.stream().noneMatch(capture ->
                "connector-to-server".equals(capture.direction())
                        && capture.ioa() == SuiteConstants.IEC_CORE_TO_EXTERNAL_IOA
                        && close(capture.value(), state.iecCoreToExternalMeasurement.value()))) {
            failures.add("IEC connector did not send expected core-to-external ASDU");
        }
        requireCoreQuery(state.localCore, SuiteConstants.IEC_CORE_TO_EXTERNAL_TS, "last=365d", failures);
        requireCoreQuery(state.localCore, SuiteConstants.IEC_CORE_TO_EXTERNAL_TS, "order=desc", failures);
        requireCoreQuery(state.localCore, SuiteConstants.IEC_CORE_TO_EXTERNAL_TS, "limit=1", failures);
    }

    private static void verifyIcc(HarnessState state, List<String> failures) {
        requireToken(state, "icc-core", failures);
        requireToken(state, "icc-external", failures);
        requireMeasurement(state.externalCore, new MeasurementRecord(
                SuiteConstants.ICC_EXTERNAL_TARGET_TS,
                state.iccLocalSourceMeasurement.observedAt(),
                state.iccLocalSourceMeasurement.value()), failures);
        requireMeasurement(state.localCore, new MeasurementRecord(
                SuiteConstants.ICC_LOCAL_TARGET_TS,
                state.iccExternalSourceMeasurement.observedAt(),
                state.iccExternalSourceMeasurement.value()), failures);
        requireCoreQuery(state.localCore, SuiteConstants.ICC_LOCAL_SOURCE_TS, "last=60s", failures);
        requireCoreQuery(state.externalCore, SuiteConstants.ICC_EXTERNAL_SOURCE_TS, "last=60s", failures);
    }

    private static boolean hasExpectedTstpPutMeasurements(HarnessState state, TstpRequest request) {
        List<MeasurementRecord> measurements = request.measurements();
        return measurements.size() == 2
                && measurements.get(0).observedAt().equals(state.tstpWriterFirst.observedAt())
                && close(measurements.get(0).value(), state.tstpWriterFirst.value())
                && measurements.get(1).observedAt().equals(state.tstpWriterSecond.observedAt())
                && close(measurements.get(1).value(), state.tstpWriterSecond.value());
    }

    private static void verifyUnexpectedWrites(HarnessState state, Set<Scenario> scenarios, List<String> failures) {
        Set<UUID> allowed = new HashSet<>();
        if (scenarios.contains(Scenario.FTP)) {
            allowed.addAll(SuiteConstants.FTP_WRITE_IDS);
        }
        if (scenarios.contains(Scenario.TSTP)) {
            allowed.addAll(SuiteConstants.TSTP_WRITE_IDS);
        }
        if (scenarios.contains(Scenario.IEC)) {
            allowed.addAll(SuiteConstants.IEC_WRITE_IDS);
        }
        if (scenarios.contains(Scenario.ICC)) {
            allowed.addAll(SuiteConstants.ICC_WRITE_IDS);
        }
        for (FakeCoreState core : state.cores()) {
            for (MeasurementRecord write : core.writes()) {
                if (!allowed.contains(write.timeSeriesId())) {
                    failures.add("Unexpected write to " + core.name() + " core: " + write);
                }
            }
        }
        for (FakeCoreState core : state.cores()) {
            for (CoreRequest request : core.requests()) {
                if (!("Bearer " + SuiteConstants.TOKEN).equals(request.authorization())) {
                    failures.add("Core request missing bearer token: " + request);
                }
            }
        }
    }

    private static void requireToken(HarnessState state, String clientId, List<String> failures) {
        if (state.tokenRequests.stream().noneMatch(request -> clientId.equals(request.clientId()))) {
            failures.add("Missing token request for client_id=" + clientId);
        }
    }

    private static void requireTstpCommand(
            HarnessState state,
            String command,
            String zrid,
            int stationId,
            List<String> failures) {
        boolean found = state.tstpRequests.stream().anyMatch(request ->
                command.equalsIgnoreCase(request.command())
                        && Integer.toString(stationId).equals(request.stationId()));
        if (!found) {
            failures.add("Missing TSTP " + command + " for station " + stationId + " / " + zrid);
        }
    }

    private static void requireWriteCount(FakeCoreState core, UUID timeSeriesId, long expected, List<String> failures) {
        long actual = core.writes().stream()
                .filter(write -> timeSeriesId.equals(write.timeSeriesId()))
                .count();
        if (actual != expected) {
            failures.add("Expected " + expected + " write(s) for " + timeSeriesId + " on " + core.name()
                    + " core, got " + actual);
        }
    }

    private static void requireMeasurement(FakeCoreState core, MeasurementRecord expected, List<String> failures) {
        boolean found = core.writes().stream().anyMatch(write ->
                expected.timeSeriesId().equals(write.timeSeriesId())
                        && (expected.observedAt() == null || expected.observedAt().equals(write.observedAt()))
                        && close(expected.value(), write.value()));
        if (!found) {
            failures.add("Missing expected measurement on " + core.name() + " core: " + expected);
        }
    }

    private static void rejectValue(FakeCoreState core, UUID timeSeriesId, double value, List<String> failures) {
        boolean found = core.writes().stream().anyMatch(write ->
                timeSeriesId.equals(write.timeSeriesId()) && close(write.value(), value));
        if (found) {
            failures.add("Unexpected value " + value + " was written to " + timeSeriesId + " on " + core.name() + " core");
        }
    }

    private static void requireCoreQuery(FakeCoreState core, UUID timeSeriesId, String queryPart, List<String> failures) {
        String path = "/api/v1/time-series/" + timeSeriesId + "/measurements";
        boolean found = core.requests().stream().anyMatch(request ->
                "GET".equals(request.method())
                        && path.equals(request.path())
                        && request.query() != null
                        && request.query().contains(queryPart));
        if (!found) {
            failures.add("Missing Core GET query on " + core.name() + " for " + timeSeriesId + " containing " + queryPart);
        }
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) <= TOLERANCE;
    }

    static String summary(HarnessState state) {
        return "tokens=" + state.tokenRequests.size()
                + ", localWrites=" + state.localCore.writes().size()
                + ", externalWrites=" + state.externalCore.writes().size()
                + ", tstpRequests=" + state.tstpRequests.size()
                + ", iecCaptures=" + state.iecCaptures.size();
    }
}
