package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
                ftp.address=127.0.0.2
                ftp.port=21
                ftp.user=test-user
                ftp.password=test-pass
                ftp.path=/incoming
                parser.type=zrxp
                zrxp.parameter=Abfluss
                read.delay=15m
                timeSeriesId=11111111-1111-1111-1111-111111111111
                """);

        ConnectorOptions options = invokeStatic("getConnectorOptions", new Class<?>[]{String.class}, tmp.toString());

        assertEquals("127.0.0.1", options.coreAddress().getHostAddress());
        assertEquals(8081, options.corePort());
        assertEquals("127.0.0.2", options.pegelAddress().getHostAddress());
        assertEquals(21, options.pegelPort());
        assertEquals("test-user", options.username());
        assertEquals("test-pass", options.password());
        assertEquals("/incoming", options.path());
        assertEquals(ParserType.ZRXP, options.parserType());
        assertEquals("Abfluss", options.parameter());
        assertEquals(Duration.ofMinutes(15), options.readDelay());
        assertEquals(TIME_SERIES_ID, options.timeSeriesId());
        assertEquals(tmp.resolve("pegelhub.yaml").toString(), options.propertiesFile());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = Main.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}
