package at.pegelhub.shared.api;

import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.quality.domain.ProfileConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ConfigurationYamlTest {
    private final ConfigurationYaml yaml = new ConfigurationYaml();

    @Test
    void roundTripsOnlyNonSecretConfiguration() {
        var profile = new ProfileConfig("Test", false, List.of(UUID.randomUUID()), null, null, null, List.of(), null);
        assertThat(yaml.read(yaml.write(profile), ProfileConfig.class)).isEqualTo(profile);

        var destination = new DestinationConfig("Mail", false, DestinationConfig.Transport.SMTP, "relay",
                new DestinationConfig.MailRoute("sender@example.test", List.of("recipient@example.test"), null), null);
        assertThat(yaml.read(yaml.write(destination), DestinationConfig.class)).isEqualTo(destination);
        assertThat(yaml.write(destination)).contains("credentialRef:", "relay").doesNotContain("password", "community");

        assertThatThrownBy(() -> yaml.read(yaml.write(destination) + "password: forbidden\n", DestinationConfig.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAndOversizedDocuments() {
        assertThatThrownBy(() -> yaml.read("null", ProfileConfig.class)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> yaml.read("x".repeat(65537), ProfileConfig.class)).isInstanceOf(IllegalArgumentException.class);
    }
}
