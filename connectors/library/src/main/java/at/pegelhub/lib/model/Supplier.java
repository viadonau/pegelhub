package at.pegelhub.lib.model;

import java.io.StringBufferInputStream;
import java.sql.Timestamp;
import java.util.Calendar;

public class Supplier {

    //should be UUID
    private String id;
    private String stationNumber;
    private String stationNation;
    private String stationOwner;
    private String stationID;
    private String stationNameShort;
    private String stationNameLong;
    private String stationWaterShort;
    private String stationWaterLong;
    private String stationWaterType;
    private float stationWaterKilometer;
    private String stationWaterside;
    private float stationWaterLatitude;
    private float stationWaterLongitude;
    private float stationWaterLatitudem;
    private float stationWaterLongitudem;
    //needs to be changed to StationManufacturer, doesn't exist yet
    private StationManufacturer stationManufacturer;
    private String refreshRate;
    private String accuracy;
    private String mainUsage;
    private String dataCritically;
    //100-jähriges Hochwasser
    private int hsw100;
    //Hochwasser
    private int hsw;
    private String hswReference;
    //Mittelwasser
    private int mw;
    private String mwReference;
    //Regulierungs Niederwasserstand
    private int rnw;
    private String rnwReference;
    private float hsq100;
    private float hsq;
    private float mq;
    private float rnq;
    private float measurementAccuracy;
    private Calendar timestamp;
    private Boolean isSummertime;
    private Boolean utcIsUsed;
    private String kindOfTime;
    private String channelUse;

    public String getChannelUse() {
        return channelUse;
    }

    public void setChannelUse(String channelUse) {
        this.channelUse = channelUse;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStationNumber() {
        return stationNumber;
    }

    public void setStationNumber(String stationNumber) {
        this.stationNumber = stationNumber;
    }

    public String getStationNation() {
        return stationNation;
    }

    public void setStationNation(String stationNation) {
        this.stationNation = stationNation;
    }

    public String getStationOwner() {
        return stationOwner;
    }

    public void setStationManufacturer(StationManufacturer stationManufacturer) {
        this.stationManufacturer = stationManufacturer;
    }

    public void setStationOwner(String stationOwner) {
        this.stationOwner = stationOwner;
    }

    public String getStationID() {
        return stationID;
    }

    public void setStationID(String stationID) {
        this.stationID = stationID;
    }

    public String getStationNameShort() {
        return stationNameShort;
    }

    public void setStationNameShort(String stationNameShort) {
        this.stationNameShort = stationNameShort;
    }

    public String getStationNameLong() {
        return stationNameLong;
    }

    public void setStationNameLong(String stationNameLong) {
        this.stationNameLong = stationNameLong;
    }

    public String getStationWaterShort() {
        return stationWaterShort;
    }

    public void setStationWaterShort(String stationWaterShort) {
        this.stationWaterShort = stationWaterShort;
    }

    public String getStationWaterLong() {
        return stationWaterLong;
    }

    public void setStationWaterLong(String stationWaterLong) {
        this.stationWaterLong = stationWaterLong;
    }

    public String getStationWaterType() {
        return stationWaterType;
    }

    public void setStationWaterType(String stationWaterType) {
        this.stationWaterType = stationWaterType;
    }

    public float getStationWaterKilometer() {
        return stationWaterKilometer;
    }

    public void setStationWaterKilometer(float stationWaterKilometer) {
        this.stationWaterKilometer = stationWaterKilometer;
    }

    public String getStationWaterside() {
        return stationWaterside;
    }

    public void setStationWaterside(String stationWaterside) {
        this.stationWaterside = stationWaterside;
    }

    public float getStationWaterLatitude() {
        return stationWaterLatitude;
    }

    public void setStationWaterLatitude(float stationWaterLatitude) {
        this.stationWaterLatitude = stationWaterLatitude;
    }

    public float getStationWaterLongitude() {
        return stationWaterLongitude;
    }

    public void setStationWaterLongitude(float stationWaterLongitude) {
        this.stationWaterLongitude = stationWaterLongitude;
    }

    public float getStationWaterLatitudem() {
        return stationWaterLatitudem;
    }

    public void setStationWaterLatitudem(float stationWaterLatitudem) {
        this.stationWaterLatitudem = stationWaterLatitudem;
    }

    public float getStationWaterLongitudem() {
        return stationWaterLongitudem;
    }

    public void setStationWaterLongitudem(float stationWaterLongitudem) {
        this.stationWaterLongitudem = stationWaterLongitudem;
    }

    public StationManufacturer getStationManufacturer() {
        return stationManufacturer;
    }

    public String getRefreshRate() {
        return refreshRate;
    }

    public void setRefreshRate(String refreshRate) {
        this.refreshRate = refreshRate;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getMainUsage() {
        return mainUsage;
    }

    public void setMainUsage(String mainUsage) {
        this.mainUsage = mainUsage;
    }

    public String getDataCritically() {
        return dataCritically;
    }

    public void setDataCritically(String dataCritically) {
        this.dataCritically = dataCritically;
    }

    public int getHsw100() {
        return hsw100;
    }

    public void setHsw100(int hsw100) {
        this.hsw100 = hsw100;
    }

    public int getHsw() {
        return hsw;
    }

    public void setHsw(int hsw) {
        this.hsw = hsw;
    }

    public String getHswReference() {
        return hswReference;
    }

    public void setHswReference(String hswReference) {
        this.hswReference = hswReference;
    }

    public int getMw() {
        return mw;
    }

    public void setMw(int mw) {
        this.mw = mw;
    }

    public String getMwReference() {
        return mwReference;
    }

    public void setMwReference(String mwReference) {
        this.mwReference = mwReference;
    }

    public int getRnw() {
        return rnw;
    }

    public void setRnw(int rnw) {
        this.rnw = rnw;
    }

    public String getRnwReference() {
        return rnwReference;
    }

    public void setRnwReference(String rnwReference) {
        this.rnwReference = rnwReference;
    }

    public float getHsq100() {
        return hsq100;
    }

    public void setHsq100(float hsq100) {
        this.hsq100 = hsq100;
    }

    public float getHsq() {
        return hsq;
    }

    public void setHsq(float hsq) {
        this.hsq = hsq;
    }

    public float getMq() {
        return mq;
    }

    public void setMq(float mq) {
        this.mq = mq;
    }

    public float getRnq() {
        return rnq;
    }

    public void setRnq(float rnq) {
        this.rnq = rnq;
    }

    public float getMeasurementAccuracy() {
        return measurementAccuracy;
    }

    public void setMeasurementAccuracy(float measurementAccuracy) {
        this.measurementAccuracy = measurementAccuracy;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSummertime() {
        return isSummertime;
    }

    public void setSummertime(Boolean summertime) {
        isSummertime = summertime;
    }

    public Boolean getUtcIsUsed() {
        return utcIsUsed;
    }

    public void setUtcIsUsed(Boolean utcIsUsed) {
        this.utcIsUsed = utcIsUsed;
    }

    public String getKindOfTime() {
        return kindOfTime;
    }

    public void setKindOfTime(String kindOfTime) {
        this.kindOfTime = kindOfTime;
    }
}
