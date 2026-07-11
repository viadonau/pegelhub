package at.pegelhub.connector.livetest;

import java.time.Instant;
import java.util.List;

record TokenRequest(String serverName, String clientId, Instant receivedAt) {
}

record CoreRequest(String coreName, String method, String path, String query, String authorization, Instant receivedAt) {
}

record TstpRequest(String command, String stationId, String zrid, String rawQuery, String body,
                   List<MeasurementRecord> measurements, Instant receivedAt) {
}

record IecCapture(String direction, int ioa, double value, Instant receivedAt) {
}
