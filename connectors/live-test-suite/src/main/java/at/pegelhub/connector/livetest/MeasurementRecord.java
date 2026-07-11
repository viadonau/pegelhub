package at.pegelhub.connector.livetest;

import java.time.Instant;
import java.util.UUID;

record MeasurementRecord(UUID timeSeriesId, Instant observedAt, double value) {
}
