package at.pegelhub.lib.internal;

import java.util.Map;

public interface ApplicationProperties {
    boolean isSupplier();

    int getStationId();

    String getTokenUrl();

    String getClientId();

    String getClientSecret();

    boolean isRefreshNecessary();

    Map<String, Object> getProperties();

    void put(String key, Object value);
}
