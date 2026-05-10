package at.pegelhub.connector.ftp.fileparsing.implementation;

import at.pegelhub.connector.ftp.fileparsing.Entry;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ASCEntry implements Entry {
    private String parameter;
    private String location;
    private String subLocation;
    private String defKind;
    private String origin;
    private String source;
    private String rowKind;
    private String version;
    private String x;
    private String y;
    private String unit;
    private String accuracy;
    private String tolerance;
    private String NWlimit;
    private String comment;
    private String height;
    private boolean mainRow;
    private final HashMap<Date, String> values;

    public ASCEntry() {
        values = new HashMap<>();
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSubLocation() {
        return subLocation;
    }

    public void setSubLocation(String subLocation) {
        this.subLocation = subLocation;
    }

    public String getDefKind() {
        return defKind;
    }

    public void setDefKind(String defKind) {
        this.defKind = defKind;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRowKind() {
        return rowKind;
    }

    public void setRowKind(String rowKind) {
        this.rowKind = rowKind;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getTolerance() {
        return tolerance;
    }

    public void setTolerance(String tolerance) {
        this.tolerance = tolerance;
    }

    public String getNWlimit() {
        return NWlimit;
    }

    public void setNWlimit(String NWlimit) {
        this.NWlimit = NWlimit;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public boolean isMainRow() {
        return mainRow;
    }

    public void setMainRow(boolean mainRow) {
        this.mainRow = mainRow;
    }

    public Map<Date, String> getValues() {
        return values;
    }

    public Map<String, String> getInfos() {
        return Arrays.stream(this.getClass().getDeclaredFields())
            .filter(f -> !f.getName().equals("values"))
            .filter(f -> {
                f.setAccessible(true);
                try {
                    return f.get(this) != null;
                } catch (IllegalAccessException e) {
                    return false;
                }
            })
            .collect(Collectors.toMap(Field::getName, f -> {
                try {
                    f.setAccessible(true);
                    return f.get(this).toString();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }));
    }
}
