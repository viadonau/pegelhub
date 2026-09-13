package at.pegelhub.notifications.application;

import at.pegelhub.notifications.application.NotificationProperties.Credential;
import at.pegelhub.notifications.domain.DestinationConfig;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves allowlisted deployment credentials without copying secret values into destinations or deliveries.
 * Validation is deferred until use so an unavailable optional transport cannot prevent Core startup.
 */
@Component
public class CredentialCatalog {

    private final NotificationProperties properties;

    public CredentialCatalog(NotificationProperties properties) {
        this.properties = properties;
    }

    public record Reference(String name, String kind, boolean available) { }

    /** Exposes configuration availability, not a connectivity probe or any credential values. */
    public List<Reference> references() {
        return properties.credentials().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Reference(entry.getKey(), entry.getValue().kind(), available(entry.getValue())))
                .toList();
    }

    public void requireReference(DestinationConfig config) {
        var credential = properties.credentials().get(config.credentialRef());
        if (credential == null || !expected(config).equals(credential.kind())) {
            throw new IllegalArgumentException("Unknown or incompatible credential reference");
        }
    }

    /** Resolves the current secret behind the snapshotted reference, allowing deployment-managed rotation. */
    public Credential resolve(DestinationConfig config) {
        requireReference(config);

        var credential = properties.credentials().get(config.credentialRef());
        if (!available(credential)) {
            throw new IllegalStateException("Transport credentials are unavailable or invalid");
        }

        return credential;
    }

    private String expected(DestinationConfig config) {
        return config.transport() == DestinationConfig.Transport.SMTP ? "SMTP"
                : "v3".equals(config.snmp().version()) ? "SNMP_V3" : "SNMP_V2C";
    }

    private boolean available(Credential credential) {
        if (credential == null || credential.kind() == null) {
            return false;
        }

        return switch (credential.kind()) {
            case "SMTP" -> present(credential.host()) && credential.port() > 0 && credential.port() <= 65535
                    && (!present(credential.username()) || present(credential.password()));
            case "SNMP_V2C" -> present(credential.community());
            case "SNMP_V3" -> validV3(credential);
            default -> false;
        };
    }

    private boolean validV3(Credential credential) {
        if (!present(credential.username()) || credential.username().getBytes(StandardCharsets.UTF_8).length > 32
                || credential.securityLevel() == null
                || !Set.of("noAuthNoPriv", "authNoPriv", "authPriv").contains(credential.securityLevel())) {
            return false;
        }

        if (credential.securityLevel().equals("noAuthNoPriv")) {
            return true;
        }
        if (!validAuthentication(credential)) {
            return false;
        }

        return !credential.securityLevel().equals("authPriv") || validPrivacy(credential);
    }

    private boolean validAuthentication(Credential credential) {
        return credential.authProtocol() != null
                && Set.of("SHA", "SHA256", "MD5").contains(credential.authProtocol())
                && password(credential.authPassword())
                && (!credential.authProtocol().equals("MD5") || credential.allowLegacyAlgorithms());
    }

    private boolean validPrivacy(Credential credential) {
        return credential.privProtocol() != null
                && Set.of("AES", "AES256", "DES").contains(credential.privProtocol())
                && password(credential.privPassword())
                && (!credential.privProtocol().equals("DES") || credential.allowLegacyAlgorithms());
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean password(String value) {
        return value != null && value.length() >= 8;
    }
}
