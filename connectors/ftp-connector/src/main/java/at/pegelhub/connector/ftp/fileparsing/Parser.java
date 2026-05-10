package at.pegelhub.connector.ftp.fileparsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public interface Parser {
    ParserType getType();
    Stream<Entry> parse(InputStream is) throws IOException;
}
