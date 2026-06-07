package at.pegelhub.lib.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ApplicationPropertiesImpl implements ApplicationProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPropertiesImpl.class);
    private final boolean isSupplier;

    private final Map<String, Object> data;
    private final ObjectMapper mapper;
    private final File file;

    public ApplicationPropertiesImpl(String propertiesFile) {
        this.mapper = new YAMLMapper();
        this.mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.file = new File(propertiesFile);
        try {
            this.data = mapper.readValue(file, new TypeReference<>() { });
            this.isSupplier = (boolean) data.get("isSupplier");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSupplier() {
        return isSupplier;
    }

    @Override
    public int getStationId() {
        Map<String, Object> metadata = (Map<String, Object>) data.get(isSupplier ? "supplier" : "taker");
        return (Integer) metadata.get("id");
    }

    @Override
    public boolean isRefreshNecessary() {
        return false;
    }

    @Override
    public String getTokenUrl() {
        return keycloakValue("tokenUrl");
    }

    @Override
    public String getClientId() {
        return keycloakValue("clientId");
    }

    @Override
    public String getClientSecret() {
        return keycloakValue("clientSecret");
    }

    private String keycloakValue(String key) {
        Map<String, Object> keycloak = (Map<String, Object>) data.get("keycloak");
        return keycloak == null ? null : (String) keycloak.get(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
        syncProperties();
    }

    private void syncProperties() {
        try {
            this.mapper.writeValue(this.file, this.data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getProperties() {
        return this.data;
    }
}
