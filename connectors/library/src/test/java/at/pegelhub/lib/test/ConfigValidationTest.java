package at.pegelhub.lib.test;

import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.LoadedMapping;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidationTest {
    @Test
    void textValuesMustBeConfigured() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText(null, "demo.value"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText("", "demo.value"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText(" ", "demo.value"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText("\t\n", "demo.value"));
        assertEquals(" value ", ConfigValidation.requireText(" value ", "demo.value"));
    }

    @Test
    void countsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requirePositive(0, "demo.count"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requirePositive(-1, "demo.count"));
        assertEquals(1, ConfigValidation.requirePositive(1, "demo.count"));
        assertEquals(Integer.MAX_VALUE, ConfigValidation.requirePositive(Integer.MAX_VALUE, "demo.count"));
    }

    @Test
    void durationsMustBePresentAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requirePositive(null, "demo.interval"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requirePositive(Duration.ZERO, "demo.interval"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requirePositive(Duration.ofNanos(-1), "demo.interval"));

        Duration interval = Duration.ofNanos(1);
        assertSame(interval, ConfigValidation.requirePositive(interval, "demo.interval"));
    }

    @Test
    void tcpPortsMustUseValidRange() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireTcpPort(0, "demo.port"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireTcpPort(65536, "demo.port"));
        assertEquals(65535, ConfigValidation.requireTcpPort(65535, "demo.port"));
    }

    @Test
    void coreAuthenticationRequiresTextValues() {
        assertThrows(IllegalArgumentException.class, () -> new CoreAuthentication(" ", "connector", "secret"));
        assertThrows(IllegalArgumentException.class,
                () -> new CoreAuthentication("http://keycloak.local/token", "", "secret"));
        assertThrows(IllegalArgumentException.class,
                () -> new CoreAuthentication("http://keycloak.local/token", "connector", null));
    }

    @Test
    void credentialsAreNotTrimmed() {
        String secret = " #secret: [value]\t ";
        var authentication = new CoreAuthentication("http://keycloak.local/token", "connector", secret);

        assertEquals(secret, authentication.clientSecret());
    }

    @Test
    void mappingFileNamesMustBePresentWithoutChangingTheirWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> new LoadedMapping<>(null, "mapping"));
        assertThrows(IllegalArgumentException.class, () -> new LoadedMapping<>(" ", "mapping"));
        assertEquals(" mapping.yaml ", new LoadedMapping<>(" mapping.yaml ", "mapping").fileName());
    }
}
