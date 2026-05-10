package at.pegelhub.connector.ma.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaConfigLoaderTest {

    @TempDir
    Path tmp;

    private Path writeProps(List<String> lines) throws IOException {
        Path p = tmp.resolve("connector.properties");
        Files.write(p, lines);
        return p;
    }

    @Test
    void shouldLoadAllFieldsFromValidProperties() throws Exception {
        Path cfg = writeProps(List.of(
                "Core.IP=127.0.0.1",
                "Core.Port=8080",
                "DelayInterval=5s",
                "InputsDir=" + tmp.resolve("inputs")
        ));

        MaConnectorOptions opts = new MaConfigLoader().parseConfig(new String[]{tmp.toString()});

        assertEquals("127.0.0.1", opts.coreAddress());
        assertEquals(8080, opts.corePort());
        assertEquals(Duration.ofSeconds(5), opts.delay());
        assertTrue(opts.inputsDir().endsWith("inputs"));
    }

    @Test
    void shouldParseDelayUnitsCaseInsensitively() throws Exception {
        Path minutes = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=8081",
                "DelayInterval=2M",
                "InputsDir=" + tmp.toString()
        ));
        MaConnectorOptions optM = new MaConfigLoader().parseConfig(new String[]{tmp.toString()});
        assertEquals(Duration.ofMinutes(2), optM.delay());

        Path hours = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=8081",
                "DelayInterval=1H",
                "InputsDir=" + tmp.toString()
        ));
        MaConnectorOptions optH = new MaConfigLoader().parseConfig(new String[]{tmp.toString()});
        assertEquals(Duration.ofHours(1), optH.delay());
    }

    @Test
    void shouldFailOnUnknownDelayUnit() throws Exception {
        Path cfg = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=8081",
                "DelayInterval=10x",
                "InputsDir=/tmp"
        ));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new MaConfigLoader().parseConfig(new String[]{tmp.toString()}));
        assertTrue(ex.getMessage().contains("Unknown unit"));
    }

    @Test
    void shouldFailOnInvalidPort() throws Exception {
        Path cfg = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=not-a-number",
                "DelayInterval=1s",
                "InputsDir=/tmp"
        ));
        assertThrows(NumberFormatException.class,
                () -> new MaConfigLoader().parseConfig(new String[]{tmp.toString()}));
    }

    @Test
    void shouldFailWhenRequiredPropertyIsMissing() throws Exception {
        Path cfg = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=8081",
                "DelayInterval=1s"
                // missing InputsDir
        ));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new MaConfigLoader().parseConfig(new String[]{tmp.toString()}));
        assertTrue(ex.getMessage().contains("Missing or empty property"));
    }

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        MaConfigLoader loader = new MaConfigLoader();

        assertEquals("/app/config", readStaticField("DEFAULT_CONFIG_DIR"));
        assertEquals("/app/config", invoke(loader, "getConfigDir", new Class<?>[]{String[].class}, (Object) new String[0]));
        assertEquals(Path.of("/tmp/ma-config", "connector.properties").toString(),
                invoke(loader, "resolveConfigPath", new Class<?>[]{String.class}, "/tmp/ma-config"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = MaConfigLoader.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    private static Object readStaticField(String name) throws Exception {
        Field field = MaConfigLoader.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
