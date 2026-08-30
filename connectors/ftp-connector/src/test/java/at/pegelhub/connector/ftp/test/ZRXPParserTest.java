package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZRXPParserTest {
    @Test
    public void createsEntryForSingleBlockFile() throws IOException {
        var input = """
                #CDASANAMEWien Schwedenbrücke|*|
                #REXCHANGEHYDAMSEX_10001033_Wasserstand|*|CUNITcm|*|RINVAL-777|*|
                20260625070000 282
                20260625071500 283
                """;

        var parser = ParserFactory.getParser(ParserType.ZRXP);
        var entries = parser.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))).toList();

        assertEquals(1, entries.size());
        assertEquals("10001033", entries.getFirst().getInfos().get("location"));
        assertEquals("Wasserstand", entries.getFirst().getInfos().get("parameter"));
        assertEquals(2, entries.getFirst().getValues().size());
    }

    @Test
    public void skipsValuesMatchingTheDeclaredInvalidSentinel() throws IOException {
        var input = """
                #REXCHANGEHYDAMSEX_10001033_Wasserstand|*|CUNITcm|*|RINVAL-777|*|
                20260625070000 282
                20260625071500 -777.0
                """;

        var parser = ParserFactory.getParser(ParserType.ZRXP);
        var entry = parser.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .findFirst()
                .orElseThrow();

        assertEquals(1, entry.getValues().size());
        assertEquals("282", entry.getValues().values().iterator().next());
    }
}
