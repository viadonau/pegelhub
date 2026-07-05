package at.pegelhub.measurement.persistence;

final class InfluxMeasurementSchema {

    static final String VALUE_FIELD = "value";
    static final String RECEIVED_AT_FIELD = "receivedAt";
    static final String SUBMITTED_BY_CONNECTOR_ID_TAG = "submittedByConnectorId";

    private InfluxMeasurementSchema() {
        throw new IllegalStateException("utility class can not be initialized.");
    }
}
