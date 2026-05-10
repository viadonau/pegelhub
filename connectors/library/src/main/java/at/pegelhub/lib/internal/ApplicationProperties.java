package at.pegelhub.lib.internal;

import at.pegelhub.lib.internal.dto.SupplierSendDto;
import at.pegelhub.lib.internal.dto.TakerSendDto;

import java.util.Map;

public interface ApplicationProperties {
    boolean isSupplier();
    SupplierSendDto getSupplier();
    TakerSendDto getTaker();

    String getApiKey();

    void setApiKey(String apiKey);

    boolean isRefreshNecessary();

    boolean isSupplierDataToSend();

    void setUtcIsUsed(Boolean utcIsUsed);

    Map<String, Object> getProperties();

    void put(String key, Object value);
}
