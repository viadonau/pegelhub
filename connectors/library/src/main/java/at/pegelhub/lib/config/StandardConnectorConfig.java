package at.pegelhub.lib.config;

import at.pegelhub.lib.CoreConnection;

import java.time.Duration;

public interface StandardConnectorConfig {
    String DEFAULT_MAPPINGS_DIRECTORY = "mappings";

    CoreConfig core();

    KeycloakConfig keycloak();

    default CoreEndpointConfig coreEndpoint() {
        return new CoreEndpointConfig(core(), keycloak());
    }

    default CoreConnection coreConnection() {
        return coreEndpoint().connection();
    }

    ScheduleConfig schedule();

    String mappingsDir();

    default String mappingsDirectory() {
        String configured = mappingsDir();
        return configured == null || configured.isBlank()
                ? DEFAULT_MAPPINGS_DIRECTORY
                : configured.trim();
    }

    default Duration scheduleInterval() {
        return schedule().interval();
    }
}
