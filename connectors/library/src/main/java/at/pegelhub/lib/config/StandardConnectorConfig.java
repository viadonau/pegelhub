package at.pegelhub.lib.config;

public interface StandardConnectorConfig {
    CoreConfig core();

    KeycloakConfig keycloak();

    default CoreEndpointConfig coreEndpoint() {
        return new CoreEndpointConfig(core(), keycloak());
    }

    ScheduleConfig schedule();

    String mappingsDir();
}
