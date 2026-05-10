package at.pegelhub.lib.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * The model class used to send and receive {@code Measurement} objects.
 */
public class Measurement {
    // should be UUID but core doesn't support it yet
    private LocalDateTime timestamp;
    private final Map<String, Double> fields;
    private final Map<String, String> infos;

    public Measurement() {
        this(new HashMap<>(), new HashMap<>());
    }
    public Measurement(Map<String, Double> fields, Map<String, String> infos) {
        this.timestamp = LocalDateTime.now();
        this.fields = fields;
        this.infos = infos;
    }
    public Measurement(LocalDateTime timestamp, Map<String, Double> fields, Map<String, String> infos) {
        this.timestamp = timestamp;
        this.fields = fields;
        this.infos = infos;
    }


    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Double> getFields() {
        return fields;
    }

    public Map<String, String> getInfos() {
        return infos;
    }


    @Override
    public String toString() {
        return "Measurement{" +
                "\n  timestamp=" + timestamp +
                ",\n  fields=" + fields +
                ",\n  infos=" + infos +
                "\n}";
    }
}
