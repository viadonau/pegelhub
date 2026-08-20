package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ASCParserTest {
    private static Parser parser;

    @BeforeAll
    public static void setup() {
        parser = ParserFactory.getParser(ParserType.ASC);
    }

    @Test
    public void createsEntryOnBeginStatement() throws IOException {
        var file = Utils.getResourceStream("Begin.asc");

        var result = parser.parse(file);

        assertNotNull(result);
    }

    @Test
    public void createsOneFullEntryOnData() throws IOException {
        var file = Utils.getResourceStream("SingleEntry.asc");

        var result = parser.parse(file);

        assertNotNull(result);
        var entries = result.toList();
        assertEquals(1, entries.size());
    }

    @Test
    public void throwsIllegalArgumentExceptionOnInvalidLine() throws IOException {
        var file = Utils.getResourceStream("GeneralError.asc");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(file));
    }

    @Test
    public void throwsIllegalArgumentExceptionOnInvalidValueLine() throws IOException {
        var file = Utils.getResourceStream("ValueError.asc");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(file));
    }

    @Test
    public void doesNotThrowOnBlankLines() throws IOException {
        var file = Utils.getResourceStream("BlankLines.asc");

        assertDoesNotThrow(() -> parser.parse(file));
    }

    @Test
    public void decodesIso88591UnitsUsedByHydrographicAscFiles() throws IOException {
        String input = """
                BEGIN
                Parameter: Abfluss
                Einheit: m\u00b3/s
                Werte:
                01.01.2024 00:00:00 1.2
                BEGIN
                Parameter: WasserstandAbs
                Einheit: m \u00fc.A.
                Werte:
                01.01.2024 00:00:00 157.3
                BEGIN
                Parameter: WTemperatur
                Einheit: \u00b0C
                Werte:
                01.01.2024 00:00:00 18.4
                """;

        var entries = parser.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.ISO_8859_1))).toList();

        assertEquals(3, entries.size());
        assertEquals("m\u00b3/s", entries.get(0).getInfos().get("unit"));
        assertEquals("m \u00fc.A.", entries.get(1).getInfos().get("unit"));
        assertEquals("\u00b0C", entries.get(2).getInfos().get("unit"));
    }
}
