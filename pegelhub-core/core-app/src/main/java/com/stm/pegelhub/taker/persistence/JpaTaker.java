package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.connector.persistence.JpaConnector;
import com.stm.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;

import javax.persistence.*;
import java.time.Duration;
import java.util.UUID;

/**
 * JPA Data class for {@code Taker}s.
 */

@Entity
@Data
@Table(name = "Taker")
public class JpaTaker extends IdentifiableEntity {

    @Column(nullable = false, length = 50)
    private String stationNumber;

    @Column(nullable = false)
    private int stationId;

    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaTakerServiceManufacturer takerServiceManufacturer;

    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaConnector connector;

    @Column(nullable = false, length = 20)
    private Duration refreshRate;

    public JpaTaker(UUID id, String stationNumber, int stationId, JpaTakerServiceManufacturer takerServiceManufacturer, JpaConnector connector, Duration refreshRate) {
        this.id = id;
        this.stationNumber = stationNumber;
        this.stationId = stationId;
        this.takerServiceManufacturer = takerServiceManufacturer;
        this.connector = connector;
        this.refreshRate = refreshRate;
    }

    public JpaTaker() {
    }
}
