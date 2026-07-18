package at.pegelhub.lib.test;

import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreAuthentication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidationTest {
    @Test
    void textValuesMustBeConfigured() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText(null, "demo.value"));
        assertThrows(IllegalArgumentException.class, () -> ConfigValidation.requireText(" ", "demo.value"));
        assertEquals(" value ", ConfigValidation.requireText(" value ", "demo.value"));
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
}
