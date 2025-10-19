package com.stm.pegelhub.lib.internal;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.model.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MockPegelHubCommunicator implements PegelHubCommunicator {

    /**
     * Generates a single Measurement object with random data.
     * @param stationNumber The station number to include in the info map.
     * @return A new Measurement object.
     */
    private Measurement createRandomMeasurement(String stationNumber) {
        // Generate a random timestamp within the last 30 days
        long randomSeconds = ThreadLocalRandom.current().nextLong(30L * 24 * 60 * 60);
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(randomSeconds);

        // Generate random measurement values within a plausible range
        Map<String, Double> fields = new HashMap<>();
        fields.put("value", ThreadLocalRandom.current().nextDouble(50.0, 1500.0)); // in m³/s

        // Add informational data
        Map<String, String> infos = new HashMap<>();
        infos.put("stationNumber", stationNumber);
        infos.put("unit_waterLevel", "m");
        infos.put("unit_flowRate", "m³/s");
        infos.put("unit_temperature", "°C");

        return new Measurement(timestamp, fields, infos);
    }

    // --- Special Implementation for Measurements ---

    @Override
    public Collection<Measurement> getMeasurements(String timespan) {
        System.out.println("STUB: getMeasurements called with timespan: " + timespan);
        List<Measurement> randomMeasurements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // Using a generic station number as none is provided
            randomMeasurements.add(createRandomMeasurement("ST-RAND-" + (i+1)));
        }
        System.out.println("STUB: Returning " + randomMeasurements.size() + " random measurements.");
        return randomMeasurements;
    }

    @Override
    public Collection<Measurement> getMeasurementsOfStation(String stationNumber, String timespan) {
        System.out.println("STUB: getMeasurementsOfStation called for station: " + stationNumber + " with timespan: " + timespan);
        List<Measurement> randomMeasurements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            randomMeasurements.add(createRandomMeasurement(stationNumber));
        }
        System.out.println("STUB: Returning " + randomMeasurements.size() + " random measurements for station " + stationNumber);
        return randomMeasurements;
    }

    @Override
    public Optional<Measurement> getLatestMeasurementOfStation() {
        return Optional.of(createRandomMeasurement("stationNumber"));
    }

    // --- Generic Implementations ---

    @Override
    public Optional<Measurement> getMeasurementByUUID(UUID uuid) {
        System.out.println("STUB: getMeasurementByUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public HashSet<Long> getMeasurementsIDsOfStation(String stationNumber, String timespan) {
        System.out.println("STUB: getMeasurementsIDsOfStation called for station: " + stationNumber);
        return null;
    }

    @Override
    public void sendMeasurements(List<Measurement> meass) {
        System.out.println("STUB: sendMeasurements called with " + (meass != null ? meass.size() : 0) + " measurements:");
        if(meass != null) {
            for (Measurement m : meass) {
                System.out.println(m);
            }
        }
        System.out.println("STUB: Doing nothing");
    }

    @Override
    public Timestamp getSystemTime() {
        System.out.println("STUB: getSystemTime called.");
        return null;
    }

    @Override
    public Optional<Measurement> getTimestampOfLastMeasurementByUUID(UUID uuid) {
        System.out.println("STUB: getTimestampOfLastMeasurementByUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public Collection<Connector> getConnectors() {
        System.out.println("STUB: getConnectors called.");
        return List.of();
    }

    @Override
    public Optional<Connector> getConnectorByUUID(UUID uuid) {
        System.out.println("STUB: getConnectorByUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public void sendConnector(Connector connector) {
        System.out.println("STUB: sendConnector called. Doing nothing.");
    }

    @Override
    public Collection<Contact> getContacts() {
        System.out.println("STUB: getContacts called.");
        return List.of();
    }

    @Override
    public Optional<Contact> getContactByUUID(UUID uuid) {
        System.out.println("STUB: getContactByUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public void sendContact(Contact contact) {
        System.out.println("STUB: sendContact called. Doing nothing.");
    }

    @Override
    public Collection<Supplier> getSuppliers() {
        System.out.println("STUB: getSuppliers called.");
        return List.of();
    }

    @Override
    public Optional<Supplier> getSupplierbyUUID(UUID uuid) {
        System.out.println("STUB: getSupplierbyUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public UUID getConnectorID(UUID uuid) {
        System.out.println("STUB: getConnectorID called for UUID: " + uuid);
        return null;
    }

    @Override
    public Collection<Telemetry> getTelemetry(String timespan) {
        System.out.println("STUB: getTelemetry called with timespan: " + timespan);
        return List.of();
    }

    @Override
    public Optional<Telemetry> getTelemetryByUUID(UUID uuid) {
        System.out.println("STUB: getTelemetryByUUID called for UUID: " + uuid);
        return Optional.empty();
    }

    @Override
    public void sendTelemetry(Telemetry tel) {
        System.out.println("STUB: sendTelemetry called. Doing nothing.");
    }

    @Override
    public void close() throws Exception {
        System.out.println("STUB: close called. Doing nothing.");
    }
}
