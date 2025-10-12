package org.stm.pegelhub.connector.ma.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        Path p = tmp.resolve("config.properties");
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

        MaConnectorOptions opts = new MaConfigLoader().parseConfig(new String[]{cfg.toString()});

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
        MaConnectorOptions optM = new MaConfigLoader().parseConfig(new String[]{minutes.toString()});
        assertEquals(Duration.ofMinutes(2), optM.delay());

        Path hours = writeProps(List.of(
                "Core.IP=localhost",
                "Core.Port=8081",
                "DelayInterval=1H",
                "InputsDir=" + tmp.toString()
        ));
        MaConnectorOptions optH = new MaConfigLoader().parseConfig(new String[]{hours.toString()});
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
                () -> new MaConfigLoader().parseConfig(new String[]{cfg.toString()}));
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
                () -> new MaConfigLoader().parseConfig(new String[]{cfg.toString()}));
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
                () -> new MaConfigLoader().parseConfig(new String[]{cfg.toString()}));
        assertTrue(ex.getMessage().contains("Missing or empty property"));
    }
}
