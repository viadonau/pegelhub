package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.ConnectorOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TstpConfigServiceImplTest {
    private static final String TEST_CORE_PROPERTIES_PATH = "test_core.properties";
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @InjectMocks
    private TstpConfigServiceImpl tstpConfigService;
    private String tstpConfigPath;

    @BeforeEach
    void setUp() throws IOException {
        tstpConfigPath = createTestPropertiesFile();
        tstpConfigService = new TstpConfigServiceImpl(tstpConfigPath, TEST_CORE_PROPERTIES_PATH);
    }

    @Test
    void testGetConnectorOptions_returnsCorrectOptions() throws Exception {
        ConnectorOptions options = tstpConfigService.getConnectorOptions();

        assertEquals("127.0.0.1", options.coreAddress());
        assertEquals(8080, options.corePort());
        assertEquals("tstp.test.com", options.tstpAddress());
        assertEquals(8030, options.tstpPort());
        assertEquals(Duration.ofHours(1), options.readDelay());
        assertEquals(TIME_SERIES_ID, options.timeSeriesId());
        assertEquals(TEST_CORE_PROPERTIES_PATH, options.propertiesFile());
    }

    @Test
    void testParseDurationString_validInputs() {
        assertEquals(Duration.ofHours(1), tstpConfigService.parseDurationString("1h"));
        assertEquals(Duration.ofMinutes(30), tstpConfigService.parseDurationString("30m"));
        assertEquals(Duration.ofSeconds(45), tstpConfigService.parseDurationString("45s"));
    }

    @Test
    void testParseDurationString_emptyString_returnsDefault() {
        assertEquals(Duration.ofHours(2), tstpConfigService.parseDurationString(""));
    }

    @Test
    void testParseDurationString_invalidUnit_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> tstpConfigService.parseDurationString("10x"));
    }

    private String createTestPropertiesFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("connector.readDelay", "1h");
        properties.setProperty("core.address", "127.0.0.1");
        properties.setProperty("core.port", "8080");
        properties.setProperty("tstp.address", "tstp.test.com");
        properties.setProperty("tstp.port", "8030");
        properties.setProperty("timeSeriesId", TIME_SERIES_ID.toString());

        Path tempFile = Files.createTempFile("test_tstp_config-", ".properties");
        try (var out = Files.newOutputStream(tempFile)) {
            properties.store(out, null);
        }
        tempFile.toFile().deleteOnExit();
        return tempFile.toString();
    }
}
