package at.pegelhub.lib.config;

public interface StandardConnectorConfig {
    CoreConfig core();

    KeycloakConfig keycloak();

    ScheduleConfig schedule();

    String mappingsDir();
}
