package at.pegelhub.notifications.application;

import at.pegelhub.notifications.domain.DestinationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialCatalogTest {
    @ParameterizedTest
    @CsvSource({
            "noAuthNoPriv, , , false, , , true",
            "authNoPriv, SHA, , false, password, , true",
            "authNoPriv, SHA256, , false, password, , true",
            "authNoPriv, MD5, , false, password, , false",
            "authNoPriv, MD5, , true, password, , true",
            "authPriv, SHA, AES, false, password, password, true",
            "authPriv, SHA256, AES256, false, password, password, true",
            "authPriv, MD5, DES, true, password, password, true",
            "authPriv, SHA, DES, false, password, password, false",
            "authPriv, SHA, AES, false, short, password, false",
            "authPriv, SHA, AES, false, password, short, false",
            "authPriv, unknown, AES, false, password, password, false",
            "authPriv, SHA, unknown, false, password, password, false",
            "unknown, SHA, AES, false, password, password, false"
    })
    void availabilityRequiresOnlyTheSelectedSecurityLevelsCredentials(
            String level, String auth, String privacy, boolean legacy,
            String authPassword, String privacyPassword, boolean expected) {
        var credential = new NotificationProperties.Credential("SNMP_V3", null, 0, false, false,
                "test-user", null, null, level, auth, authPassword, privacy, privacyPassword, legacy);
        var catalog = new CredentialCatalog(new NotificationProperties(true, 90, Map.of("receiver", credential)));

        assertThat(catalog.references()).containsExactly(new CredentialCatalog.Reference("receiver", "SNMP_V3", expected));
    }

    @Test
    void unavailableCredentialsRemainVisibleButFailOnlyWhenResolved() {
        var credential = new NotificationProperties.Credential("SMTP", null, 25, false, false,
                null, null, null, null, null, null, null, null, false);
        var catalog = new CredentialCatalog(new NotificationProperties(true, 90, Map.of("relay", credential)));
        var destination = new DestinationConfig("Operators", true, DestinationConfig.Transport.SMTP, "relay",
                new DestinationConfig.MailRoute("ph@example.test", List.of("operator@example.test"), null), null);

        catalog.requireReference(destination);
        assertThat(catalog.references()).containsExactly(new CredentialCatalog.Reference("relay", "SMTP", false));
        assertThatThrownBy(() -> catalog.resolve(destination)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Transport credentials are unavailable or invalid");
    }
}
