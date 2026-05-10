package at.pegelhub.lib;

/**
 * The merged API spec containing {@code TelemetryAPI}, {@code MeasurementAPI}, {@code ContactAPI}, {@code ConnectorAPI} and {@code AutoCloseable}.
 */
public interface PegelHubCommunicator extends AutoCloseable, TelemetryAPI, MeasurementAPI, ContactAPI, ConnectorAPI, SupplierAPI {}
