package at.pegelhub.connector.ftp.fileparsing;

import at.pegelhub.connector.ftp.fileparsing.implementation.ASCParser;
import at.pegelhub.connector.ftp.fileparsing.implementation.ZRXPParser;

public class ParserFactory {
    public static Parser getParser(ParserType type) {
        return switch (type) {
            case ASC -> new ASCParser();
            case ZRXP -> new ZRXPParser();
            default -> throw new IllegalStateException("Undefined parser!");
        };
    }
}
