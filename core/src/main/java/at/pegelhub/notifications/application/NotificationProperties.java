package at.pegelhub.notifications.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/** Deployment settings, not an API response: expose only CredentialCatalog reference metadata to administrators. */
@ConfigurationProperties("pegelhub.notifications")
public record NotificationProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("90") int retentionDays,
        Map<String, Credential> credentials) {

    public NotificationProperties {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("Notification retention must be positive");
        }

        credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
    }

    /** Secret-bearing transport settings. Validated on use, not at binding time, to keep transports optional. */
    public record Credential(
            String kind,
            String host,
            @DefaultValue("587") int port,
            @DefaultValue("true") boolean startTls,
            @DefaultValue("false") boolean ssl,
            String username,
            String password,
            String community,
            String securityLevel,
            String authProtocol,
            String authPassword,
            String privProtocol,
            String privPassword,
            @DefaultValue("false") boolean allowLegacyAlgorithms) {

        @Override
        public String toString() {
            return "Credential[redacted]";
        }
    }
}
