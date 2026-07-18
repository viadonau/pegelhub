package at.pegelhub.connector.ftp.config;

import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.config.ConfigValidation;

import java.util.Objects;

public record FtpSource(
        String directory,
        ParserType parserType
) {
    public FtpSource {
        directory = ConfigValidation.requireText(directory, "ftp.source.directory");
        Objects.requireNonNull(parserType, "ftp.source.parserType");
    }
}
