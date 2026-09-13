package at.pegelhub.notifications.domain;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.snmp4j.smi.OID;

import java.util.List;

/**
 * Validated, non-secret routing settings used by both API edits and YAML import.
 * Exactly one route matches the transport; credential references are validated against the
 * deployment allowlist by the application, never replaced with secret values in this record.
 */
public record DestinationConfig(
        String name,
        boolean enabled,
        Transport transport,
        String credentialRef,
        MailRoute mail,
        SnmpRoute snmp) {

    public enum Transport { SMTP, SNMP }

    public DestinationConfig {
        text(name, 200, "name");

        if (transport == null) {
            throw new IllegalArgumentException("Transport is required");
        }
        if (credentialRef == null || !credentialRef.matches("[a-zA-Z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("Invalid credential reference");
        }

        if ((transport == Transport.SMTP && (mail == null || snmp != null))
                || (transport == Transport.SNMP && (snmp == null || mail != null))) {
            throw new IllegalArgumentException("Exactly one matching route is required");
        }
    }

    public record MailRoute(String from, List<String> recipients, String signature) {

        public MailRoute {
            address(from);

            if (recipients == null || recipients.isEmpty() || recipients.size() > 100) {
                throw new IllegalArgumentException("Select 1 to 100 recipients");
            }
            recipients = List.copyOf(recipients);
            recipients.forEach(DestinationConfig::address);

            if (signature != null && signature.length() > 1024) {
                throw new IllegalArgumentException("Signature too long");
            }
        }
    }

    public record SnmpRoute(
            String host,
            int port,
            String version,
            String trapOid,
            String messageOid,
            String enterpriseOid) {

        public SnmpRoute {
            if (host == null || !host.matches("[a-zA-Z0-9.:_-]{1,253}") || port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid SNMP target");
            }
            if (version == null || !List.of("v2c", "v3").contains(version)) {
                throw new IllegalArgumentException("Invalid SNMP version");
            }

            oid(trapOid);
            oid(messageOid);
            if (enterpriseOid != null && !enterpriseOid.isBlank()) {
                oid(enterpriseOid);
            }
        }
    }

    public static void text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max || value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }

    private static void address(String value) {
        text(value, 320, "email address");

        try {
            new InternetAddress(value, true).validate();
        } catch (AddressException exception) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    private static void oid(String value) {
        if (value == null || value.length() > 512 || !value.matches("[0-9]+(\\.[0-9]+)+") || !new OID(value).isValid()) {
            throw new IllegalArgumentException("Invalid SNMP OID");
        }
    }
}
