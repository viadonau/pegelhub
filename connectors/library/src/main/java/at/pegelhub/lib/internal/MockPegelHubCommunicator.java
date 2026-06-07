package at.pegelhub.lib.internal;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MockPegelHubCommunicator implements PegelHubCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(MockPegelHubCommunicator.class);

    /**
     * Generates a single Measurement object with random data.
     * @param timeSeriesId The TimeSeries ID to generate data for.
     * @return A new Measurement object.
     */
    private Measurement createRandomMeasurement(UUID timeSeriesId) {
        // Generate a random timestamp within the last 30 days
        long randomSeconds = ThreadLocalRandom.current().nextLong(30L * 24 * 60 * 60);
        Instant observedAt = Instant.now().minusSeconds(randomSeconds);

        return new Measurement(timeSeriesId, observedAt, ThreadLocalRandom.current().nextDouble(50.0, 1500.0));
    }

    // --- Special Implementation for Measurements ---

    @Override
    public Collection<Measurement> getMeasurements(String timespan) {
        LOG.debug("STUB: getMeasurements called with timespan: {}", timespan);
        List<Measurement> randomMeasurements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // Using a generic station number as none is provided
            randomMeasurements.add(createRandomMeasurement(UUID.randomUUID()));
        }
        LOG.debug("STUB: Returning {} random measurements.", randomMeasurements.size());
        return randomMeasurements;
    }

    @Override
    public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan) {
        LOG.debug("STUB: getMeasurementsOfTimeSeries called for TimeSeries: {} with timespan: {}", timeSeriesId, timespan);
        List<Measurement> randomMeasurements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            randomMeasurements.add(createRandomMeasurement(timeSeriesId));
        }
        return randomMeasurements;
    }

    @Override
    public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
        return Optional.of(createRandomMeasurement(timeSeriesId));
    }

    // --- Generic Implementations ---

    @Override
    public Optional<Measurement> getMeasurementByUUID(UUID uuid) {
        LOG.debug("STUB: getMeasurementByUUID called for UUID: {}", uuid);
        return Optional.empty();
    }

    @Override
    public void sendMeasurements(List<Measurement> meass) {
        LOG.debug("STUB: sendMeasurements called with {} measurements", meass != null ? meass.size() : 0);
        if(meass != null) {
            for (Measurement m : meass) {
                LOG.debug("STUB: {}", m);
            }
        }
        LOG.debug("STUB: Doing nothing");
    }

    @Override
    public Instant getSystemTime() {
        LOG.debug("STUB: getSystemTime called.");
        return null;
    }

    @Override
    public Collection<Connector> getConnectors() {
        LOG.debug("STUB: getConnectors called.");
        return List.of();
    }

    @Override
    public Optional<Connector> getConnectorByUUID(UUID uuid) {
        LOG.debug("STUB: getConnectorByUUID called for UUID: {}", uuid);
        return Optional.empty();
    }

    @Override
    public void sendConnector(Connector connector) {
        LOG.debug("STUB: sendConnector called. Doing nothing.");
    }

    @Override
    public Collection<Contact> getContacts() {
        LOG.debug("STUB: getContacts called.");
        return List.of();
    }

    @Override
    public Optional<Contact> getContactByUUID(UUID uuid) {
        LOG.debug("STUB: getContactByUUID called for UUID: {}", uuid);
        return Optional.empty();
    }

    @Override
    public void sendContact(Contact contact) {
        LOG.debug("STUB: sendContact called. Doing nothing.");
    }

    @Override
    public Collection<Supplier> getSuppliers() {
        LOG.debug("STUB: getSuppliers called.");
        return List.of();
    }

    @Override
    public Optional<Supplier> getSupplierbyUUID(UUID uuid) {
        LOG.debug("STUB: getSupplierbyUUID called for UUID: {}", uuid);
        return Optional.empty();
    }

    @Override
    public UUID getConnectorID(UUID uuid) {
        LOG.debug("STUB: getConnectorID called for UUID: {}", uuid);
        return null;
    }

    @Override
    public Collection<Telemetry> getTelemetry(String timespan) {
        LOG.debug("STUB: getTelemetry called with timespan: {}", timespan);
        return List.of();
    }

    @Override
    public Optional<Telemetry> getTelemetryByUUID(UUID uuid) {
        LOG.debug("STUB: getTelemetryByUUID called for UUID: {}", uuid);
        return Optional.empty();
    }

    @Override
    public void sendTelemetry(Telemetry tel) {
        LOG.debug("STUB: sendTelemetry called. Doing nothing.");
    }

    @Override
    public void close() throws Exception {
        LOG.debug("STUB: close called. Doing nothing.");
    }
}
