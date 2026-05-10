package at.pegelhub.connector.icc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;

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

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = Main.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}
