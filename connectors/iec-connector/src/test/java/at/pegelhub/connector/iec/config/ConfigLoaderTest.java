package at.pegelhub.connector.iec.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ConfigLoaderTest {

    @TempDir
    Path tmp;

    private Path writeProps(String body) throws IOException {
        Path p = tmp.resolve("connector.properties");
        Files.writeString(p, body);
        return p;
    }

    @Test
    void shouldLoadAllFieldsFromValidProperties() throws Exception {
        // Given
        Path props = writeProps("""
                DataPointsDir=%s
                Core.IP=core.local
                Core.Port=8080
                Iec.Host.IP=127.0.0.1
                Iec.Host.Port=2404
                Iec.CommonAddress=1
                DelayInterval=15s
                """.formatted(tmp.toString().replace("\\", "\\\\")));

        ConfigLoader loader = new ConfigLoader();

        // When
        ConnectorOptions opt = loader.parseConfig(new String[]{tmp.toString()});

        // Then
        assertThat(opt.dataPointsDir()).isEqualTo(tmp.toString());
        assertThat(opt.coreAddress()).isEqualTo("core.local");
        assertThat(opt.corePort()).isEqualTo(8080);
        assertThat(opt.iec_host().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(opt.iec_port()).isEqualTo(2404);
        assertThat(opt.common_address()).isEqualTo(1);
        assertThat(opt.delay()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void shouldParseDelayUnitsCaseInsensitively() throws Exception {
        // Given
        Path propsMinutes = writeProps("""
                DataPointsDir=%s
                Core.IP=core.local
                Core.Port=8080
                Iec.Host.IP=127.0.0.1
                Iec.Host.Port=2404
                Iec.CommonAddress=1
                DelayInterval=2M
                """.formatted(tmp.toString()));

        ConfigLoader loader = new ConfigLoader();

        // When
        ConnectorOptions optMinutes = loader.parseConfig(new String[]{tmp.toString()});

        // Then
        assertThat(optMinutes.delay()).isEqualTo(Duration.ofMinutes(2));

        // Given
        Path propsHours = writeProps("""
                DataPointsDir=%s
                Core.IP=core.local
                Core.Port=8080
                Iec.Host.IP=127.0.0.1
                Iec.Host.Port=2404
                Iec.CommonAddress=1
                DelayInterval=1H
                """.formatted(tmp.toString()));

        // When
        ConnectorOptions optHours = loader.parseConfig(new String[]{tmp.toString()});

        // Then
        assertThat(optHours.delay()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void shouldFailWhenRequiredPropertyIsMissing() throws Exception {
        // Given
        Path props = writeProps("""
                DataPointsDir=%s
                Core.IP=core.local
                Core.Port=8080
                Iec.Host.IP=127.0.0.1
                Iec.Host.Port=2404
                DelayInterval=10s
                """.formatted(tmp.toString())); // Missing Iec.CommonAddress

        ConfigLoader loader = new ConfigLoader();

        // When / Then
        assertThatThrownBy(() -> loader.parseConfig(new String[]{tmp.toString()}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Iec.CommonAddress");
    }

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        ConfigLoader loader = new ConfigLoader();

        assertThat((String) readStaticField("DEFAULT_CONFIG_DIR")).isEqualTo("/app/config");
        assertThat((String) invoke(loader, "getConfigDir", new Class<?>[]{String[].class}, (Object) new String[0])).isEqualTo("/app/config");
        assertThat((String) invoke(loader, "resolveConfigPath", new Class<?>[]{String.class}, "/tmp/iec-config"))
                .isEqualTo(Path.of("/tmp/iec-config", "connector.properties").toString());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    private static Object readStaticField(String name) throws Exception {
        Field field = ConfigLoader.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
