package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.service.impl.TstpConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @TempDir
    Path tmp;

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        assertEquals("/app/config", invokeStatic("resolveConfigDir", new Class<?>[]{String[].class}, (Object) new String[0]));
    }

    @Test
    void shouldResolveConnectorOptionsFromExplicitConfigDir() throws Exception {
        Files.writeString(tmp.resolve("connector.properties"), """
                core.address=127.0.0.1
                core.port=8081
                tstp.address=127.0.0.2
                tstp.port=8030
                connector.readDelay=10s
                """);

        ConnectorOptions options = new TstpConfigServiceImpl(
                invokeStatic("resolveConnectorConfigPath", new Class<?>[]{String.class}, tmp.toString()),
                invokeStatic("resolvePegelhubConfigPath", new Class<?>[]{String.class}, tmp.toString())
        ).getConnectorOptions();

        assertEquals("127.0.0.1", options.coreAddress());
        assertEquals(8081, options.corePort());
        assertEquals("127.0.0.2", options.tstpAddress());
        assertEquals(8030, options.tstpPort());
        assertEquals(Duration.ofSeconds(10), options.readDelay());
        assertEquals(tmp.resolve("pegelhub.yaml").toString(), options.propertiesFile());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = Main.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}
