package at.pegelhub.connector.icc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        assertEquals("/app/config", invokeStatic("getConfigDir", new Class<?>[]{String[].class}, (Object) new String[0]));
    }

    @Test
    void shouldResolveAllConfigFilesFromExplicitConfigDir() throws Exception {
        String configDir = "/tmp/icc-config";

        assertEquals(Path.of(configDir, "connector.properties").toString(),
                invokeStatic("resolveConnectorConfigPath", new Class<?>[]{String.class}, configDir));
        assertEquals(Path.of(configDir, "source-pegelhub.yaml").toString(),
                invokeStatic("resolveSourcePegelhubPath", new Class<?>[]{String.class}, configDir));
        assertEquals(Path.of(configDir, "sink-pegelhub.yaml").toString(),
                invokeStatic("resolveSinkPegelhubPath", new Class<?>[]{String.class}, configDir));
    }

    @Test
    void shouldParseCommaSeparatedTimeSeriesIds() throws Exception {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertEquals(List.of(first, second),
                invokeStatic("parseTimeSeriesIds", new Class<?>[]{String.class}, first + ", " + second));
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = Main.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}
