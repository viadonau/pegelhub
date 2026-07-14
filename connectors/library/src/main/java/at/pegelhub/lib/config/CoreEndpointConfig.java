package at.pegelhub.lib.config;

import at.pegelhub.lib.CoreConnection;

import java.util.Objects;

public record CoreEndpointConfig(CoreConfig core, KeycloakConfig keycloak) {
    public CoreEndpointConfig {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(keycloak, "keycloak");
    }

    public CoreConnection connection() {
        return new CoreConnection(core.baseUrl(), keycloak.credentials());
    }
}
