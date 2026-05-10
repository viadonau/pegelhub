package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.connector.persistence.JpaConnector;
import com.stm.pegelhub.shared.persistence.IdentifiableEntity;


import lombok.Data;

import javax.persistence.*;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Data class for {@code Supplier}s.
 */

@Entity
@Data
@Table(name = "Supplier", uniqueConstraints = {@UniqueConstraint(columnNames = "connector_id")})
public class JpaSupplier extends IdentifiableEntity {

    @OneToOne
    @JoinColumn(nullable = false)
    private JpaConnector connector;

    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaStationManufacturer stationManufacturer;

    @Column(nullable = false, length = 50)
    private String stationNumber;

    @Column(nullable = false)
    private int stationId;

    @Column(nullable = false)
    private Duration refreshRate;

    @Column(nullable = false)
    private double accuracy;

    @Column(nullable = false)
    private String mainUsage;

    @Column(nullable = false)
    private String dataCritically;

    @Column(nullable = false, length = 255)
    private String stationName;

    @Column(nullable = false, length = 255)
    private String stationWater;

    @Column(nullable = false)
    private char stationWaterType;

    // info data

    @Column()
    private double stationBaseReferenceLevel;

    @Column(length = 50)
    private String stationReferencePlace;

    @Column()
    private double stationWaterKilometer;

    @Column(length = 5)
    private String stationWaterside;

    @Column()
    private double stationWaterLatitude;

    @Column()
    private double stationWaterLongitude;

    @Column()
    private double stationWaterLatitudem;

    @Column()
    private double stationWaterLongtitudem;

    @Column()
    private double hsw100;

    @Column()
    private double hsw;

    @Column(length = 50)
    private int hswReference;

    @Column()
    private double mw;

    @Column(length = 50)
    private int mwReference;

    @Column()
    private double rnw;

    @Column(length = 50)
    private int rnwReference;

    @Column()
    private double hsq100;

    @Column()
    private double hsq;

    @Column()
    private double mq;

    @Column()
    private double rnq;

    @Column()
    private String channelUse;

    @Column()
    private Boolean utcIsUsed;

    @Column()
    private Boolean isSummertime;

    public JpaSupplier(UUID id, String stationNumber, Integer stationId,
                       String stationName, String stationWater, Character stationWaterType,
                       JpaStationManufacturer stationManufacturer, JpaConnector connector,
                       Duration refreshRate, Double accuracy, String mainUsage, String dataCritically,
                       //Info data
                       Double stationBaseReferenceLevel, String stationReferencePlace,
                       Double stationWaterKilometer, String stationWaterSide,
                       Double stationWaterLatitude, Double stationWaterLongitude,
                       Double stationWaterLatitudem, Double stationWaterLongitudem,
                       Double hsw100, Double hsw, Integer hswReference, Double mw,
                       Integer mwReference, Double rnw, Integer rnwReference,
                       Double hsq100, Double hsq, Double mq, Double rnq, String channelUse, Boolean utcIsUsed, Boolean isSummertime) {
        this.id = id;
        this.stationNumber = stationNumber;
        this.stationId = stationId;
        this.stationName = stationName;
        this.stationWater = stationWater;
        this.stationWaterType = stationWaterType;
        this.stationManufacturer = stationManufacturer;
        this.connector = connector;
        this.refreshRate = refreshRate;
        this.accuracy = accuracy;
        this.mainUsage = mainUsage;
        this.dataCritically = dataCritically;
        this.stationBaseReferenceLevel = stationBaseReferenceLevel;
        this.stationReferencePlace = stationReferencePlace;
        this.stationWaterKilometer = stationWaterKilometer;
        this.stationWaterside = stationWaterSide;
        this.stationWaterLatitude = stationWaterLatitude;
        this.stationWaterLongitude = stationWaterLongitude;
        this.stationWaterLatitudem = stationWaterLatitudem;
        this.stationWaterLongtitudem = stationWaterLongitudem;
        this.hsw100 = hsw100;
        this.hsw = hsw;
        this.hswReference = hswReference;
        this.mw = mw;
        this.mwReference = mwReference;
        this.rnw = rnw;
        this.rnwReference = rnwReference;
        this.hsq100 = hsq100;
        this.hsq = hsq;
        this.mq = mq;
        this.rnq = rnq;
        this.channelUse = channelUse;
        this.utcIsUsed = utcIsUsed;
        this.isSummertime = isSummertime;
    }

    public JpaSupplier() {
    }
}
