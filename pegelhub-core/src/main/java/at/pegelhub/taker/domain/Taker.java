package at.pegelhub.taker.domain;

import at.pegelhub.connector.domain.Connector;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Data class for takers which represents an entry in the RDBMS
 */
@Getter
@Setter
public final class Taker {
    private final UUID id;
    private String stationNumber;
    private int stationId;
    private TakerServiceManufacturer takerServiceManufacturer;
    private Connector connector;
    private Duration refreshRate;

    public Taker(UUID id, String stationNumber, Integer stationId,
                 TakerServiceManufacturer takerServiceManufacturer,
                 Connector connector, Duration refreshRate) {
        this.id = id;
        this.stationNumber = stationNumber;
        this.stationId = stationId;
        this.takerServiceManufacturer = takerServiceManufacturer;
        this.connector = connector;
        this.refreshRate = refreshRate;
    }

    public Taker() {
        this.id = null;
    }

    public Taker withId(UUID id) {
        return new Taker(id, this.stationNumber, this.stationId, this.takerServiceManufacturer,
                this.connector, this.refreshRate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Taker) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.stationNumber, that.stationNumber) &&
                Objects.equals(this.stationId, that.stationId) &&
                Objects.equals(this.takerServiceManufacturer, that.takerServiceManufacturer) &&
                Objects.equals(this.connector, that.connector) &&
                Objects.equals(this.refreshRate, that.refreshRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stationNumber, stationId, takerServiceManufacturer, connector, refreshRate);
    }

    @Override
    public String toString() {
        return "Taker[" +
                "id=" + id + ", " +
                "stationNumber=" + stationNumber + ", " +
                "stationId=" + stationId + ", " +
                "takerServiceManufacturer=" + takerServiceManufacturer + ", " +
                "connector=" + connector + ", " +
                "refreshRate=" + refreshRate + ']';
    }
}