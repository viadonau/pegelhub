package com.stm.pegelhub.supplier.domain;

import com.stm.pegelhub.connector.domain.Connector;


import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Data class for suppliers which represents an entry in the RDBMS
 */
@Getter
@Setter
public final class Supplier {
    private final UUID id;
    private String stationNumber;
    private int stationId;
    private String stationName;
    private String stationWater;
    private char stationWaterType;
    private StationManufacturer stationManufacturer;
    private Connector connector;
    private Duration refreshRate;
    private double accuracy;
    private String mainUsage;
    private String dataCritically;
    private double stationBaseReferenceLevel;
    private String stationReferencePlace;
    private double stationWaterKilometer;
    private String stationWaterSide;
    private double stationWaterLatitude;
    private double stationWaterLongitude;
    private double stationWaterLatitudem;
    private double stationWaterLongitudem;
    private double hsw100;
    private double hsw;
    private int hswReference;
    private double mw;
    private int mwReference;
    private double rnw;
    private int rnwReference;
    private double hsq100;
    private double hsq;
    private double mq;
    private double rnq;
    private Boolean utcIsUsed;
    private Boolean isSummertime;
    @Setter
    @Getter
    private String channelUse;

    public Supplier(UUID id, String stationNumber, Integer stationId,
                    String stationName, String stationWater, Character stationWaterType,
                    StationManufacturer stationManufacturer, Connector connector,
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
        this.stationWaterSide = stationWaterSide;
        this.stationWaterLatitude = stationWaterLatitude;
        this.stationWaterLongitude = stationWaterLongitude;
        this.stationWaterLatitudem = stationWaterLatitudem;
        this.stationWaterLongitudem = stationWaterLongitudem;
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

    public Supplier() {
        this.id = null;
    }

    public Supplier withId(UUID uuid) {
        return new Supplier(uuid, this.stationNumber, this.stationId, this.stationName,
                this.stationWater, this.stationWaterType, this.stationManufacturer, this.connector,
                this.refreshRate, this.accuracy, this.mainUsage, this.dataCritically, this.stationBaseReferenceLevel,
                this.stationReferencePlace, this.stationWaterKilometer, this.stationWaterSide, this.stationWaterLatitude,
                this.stationWaterLongitude, this.stationWaterLatitudem, this.stationWaterLongitudem, this.hsw100, this.hsw,
                this.hswReference, this.mw, this.mwReference, this.rnw, this.rnwReference, this.hsq100, this.hsq, this.mq, this.rnq, this.channelUse, this.utcIsUsed, this.isSummertime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Supplier) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.stationNumber, that.stationNumber) &&
                Objects.equals(this.stationId, that.stationId) &&
                Objects.equals(this.stationName, that.stationName) &&
                Objects.equals(this.stationWater, that.stationWater) &&
                Objects.equals(this.stationWaterType, that.stationWaterType) &&
                Objects.equals(this.stationManufacturer, that.stationManufacturer) &&
                Objects.equals(this.connector, that.connector) &&
                Objects.equals(this.refreshRate, that.refreshRate) &&
                Objects.equals(this.accuracy, that.accuracy) &&
                Objects.equals(this.mainUsage, that.mainUsage) &&
                Objects.equals(this.dataCritically, that.dataCritically) &&
                Objects.equals(this.stationBaseReferenceLevel, that.stationBaseReferenceLevel) &&
                Objects.equals(this.stationReferencePlace, that.stationReferencePlace) &&
                Objects.equals(this.stationWaterKilometer, that.stationWaterKilometer) &&
                Objects.equals(this.stationWaterSide, that.stationWaterSide) &&
                Objects.equals(this.stationWaterLatitude, that.stationWaterLatitude) &&
                Objects.equals(this.stationWaterLongitude, that.stationWaterLongitude) &&
                Objects.equals(this.stationWaterLatitudem, that.stationWaterLatitudem) &&
                Objects.equals(this.stationWaterLongitudem, that.stationWaterLongitudem) &&
                Objects.equals(this.hsw100, that.hsw100) &&
                Objects.equals(this.hsw, that.hsw) &&
                Objects.equals(this.hswReference, that.hswReference) &&
                Objects.equals(this.mw, that.mw) &&
                Objects.equals(this.mwReference, that.mwReference) &&
                Objects.equals(this.rnw, that.rnw) &&
                Objects.equals(this.rnwReference, that.rnwReference) &&
                Objects.equals(this.hsq100, that.hsq100) &&
                Objects.equals(this.hsq, that.hsq) &&
                Objects.equals(this.mq, that.mq) &&
                Objects.equals(this.rnq, that.rnq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stationNumber, stationId, stationName, stationWater, stationWaterType, stationManufacturer, connector, refreshRate, accuracy, mainUsage, dataCritically, stationBaseReferenceLevel, stationReferencePlace, stationWaterKilometer, stationWaterSide, stationWaterLatitude, stationWaterLongitude, stationWaterLatitudem, stationWaterLongitudem, hsw100, hsw, hswReference, mw, mwReference, rnw, rnwReference, hsq100, hsq, mq, rnq);
    }

    @Override
    public String toString() {
        return "Supplier[" +
                "id=" + id + ", " +
                "stationNumber=" + stationNumber + ", " +
                "stationId=" + stationId + ", " +
                "stationName=" + stationName + ", " +
                "stationWater=" + stationWater + ", " +
                "stationWaterType=" + stationWaterType + ", " +
                "stationManufacturer=" + stationManufacturer + ", " +
                "connector=" + connector + ", " +
                "refreshRate=" + refreshRate + ", " +
                "accuracy=" + accuracy + ", " +
                "mainUsage=" + mainUsage + ", " +
                "dataCritically=" + dataCritically + ", " +
                "stationBaseReferenceLevel=" + stationBaseReferenceLevel + ", " +
                "stationReferencePlace=" + stationReferencePlace + ", " +
                "stationWaterKilometer=" + stationWaterKilometer + ", " +
                "stationWaterSide=" + stationWaterSide + ", " +
                "stationWaterLatitude=" + stationWaterLatitude + ", " +
                "stationWaterLongitude=" + stationWaterLongitude + ", " +
                "stationWaterLatitudem=" + stationWaterLatitudem + ", " +
                "stationWaterLongitudem=" + stationWaterLongitudem + ", " +
                "hsw100=" + hsw100 + ", " +
                "hsw=" + hsw + ", " +
                "hswReference=" + hswReference + ", " +
                "mw=" + mw + ", " +
                "mwReference=" + mwReference + ", " +
                "rnw=" + rnw + ", " +
                "rnwReference=" + rnwReference + ", " +
                "hsq100=" + hsq100 + ", " +
                "hsq=" + hsq + ", " +
                "mq=" + mq + ", " +
                "rnq=" + rnq + ']';
    }
}