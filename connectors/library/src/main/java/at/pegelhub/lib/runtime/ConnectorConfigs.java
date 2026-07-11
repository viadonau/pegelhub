package at.pegelhub.lib.runtime;

import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.KeycloakConfig;
import at.pegelhub.lib.config.StandardConnectorConfig;

import java.time.Duration;

public final class ConnectorConfigs {
    public static final String CONNECTOR_CONFIG_FILE = "connector.yaml";
    public static final String DEFAULT_MAPPINGS_DIR = "mappings";

    private ConnectorConfigs() {
    }

    public static CoreConnection coreConnection(StandardConnectorConfig config) {
        return coreConnection(config.core(), config.keycloak());
    }

    public static CoreConnection coreConnection(CoreConfig core, KeycloakConfig keycloak) {
        return new CoreConnection(core.baseUrl(), keycloak.credentials());
    }

    public static Duration delay(ConnectorContext context, StandardConnectorConfig config) {
        return context.parseDuration(config.schedule().delay());
    }

    public static String mappingsDir(StandardConnectorConfig config) {
        String configured = config.mappingsDir();
        return configured == null || configured.isBlank() ? DEFAULT_MAPPINGS_DIR : configured.trim();
    }
}
