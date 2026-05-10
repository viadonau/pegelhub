package at.pegelhub.supplier.persistence;

import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.shared.persistence.IdentifiableEntity;


import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.Duration;
import java.util.UUID;

/**
 * JPA Data class for {@code Supplier}s.
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
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

    @Column(nullable = false)
    private String stationName;

    @Column(nullable = false)
    private String stationWater;

    @Column(nullable = false)
    private char stationWaterType;

    // info data

    private double stationBaseReferenceLevel;

    @Column(length = 50)
    private String stationReferencePlace;

    private double stationWaterKilometer;

    @Column(length = 5)
    private String stationWaterside;

    private double stationWaterLatitude;

    private double stationWaterLongitude;

    private double stationWaterLatitudem;

    private double stationWaterLongtitudem;

    private double hsw100;

    private double hsw;

    @Column(length = 50)
    private int hswReference;

    private double mw;

    @Column(length = 50)
    private int mwReference;

    private double rnw;

    @Column(length = 50)
    private int rnwReference;

    private double hsq100;

    private double hsq;

    private double mq;

    private double rnq;

    private String channelUse;

    private Boolean utcIsUsed;

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
