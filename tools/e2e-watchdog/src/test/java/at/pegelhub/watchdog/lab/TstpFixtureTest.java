package at.pegelhub.watchdog.lab;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TstpFixtureTest {
    @Test
    void returnsOnlyStoredPutsAndFaultsChangeThoseReturns() throws Exception {
        var fixture = new TstpFixture();
        var query = Map.of("Cmd", "Get", "ZRID", "1", "Von", "2026-09-15T12:00:00Z", "Bis", "2026-09-15T12:00:01Z");
        assertTrue(fixture.handle(query, new byte[0]).contains("ANZ=\"0\""));

        // Independent wire vector: 2026-09-15 12:00:00 UTC followed by IEEE-754 big-endian 721.
        byte[] record = HexFormat.of().parseHex("0007ea090f0c000044344000");
        String body = "<TSD><DATA><![CDATA[" + Base64.getEncoder().encodeToString(record) + "]]></DATA></TSD>";
        fixture.handle(Map.of("Cmd", "PUT"), body.getBytes(StandardCharsets.UTF_8));
        assertTrue(fixture.handle(query, new byte[0]).contains(Base64.getEncoder().encodeToString(record)));

        fixture.configure("corrupt", 60);
        record = HexFormat.of().parseHex("0007ea090f0c000044348000");
        assertTrue(fixture.handle(query, new byte[0]).contains(Base64.getEncoder().encodeToString(record)));

        fixture.configure("drop", 60);
        assertTrue(fixture.handle(query, new byte[0]).contains("ANZ=\"0\""));

        fixture.configure("delay", 60);
        assertTrue(fixture.handle(query, new byte[0]).contains("ANZ=\"0\""));
    }
}
