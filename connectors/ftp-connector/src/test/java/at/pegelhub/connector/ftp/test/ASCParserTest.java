package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;

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
}
