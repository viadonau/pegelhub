package at.pegelhub.connector.tstp.config;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TstpWriteFormat {
    BINARY, ASCII;

    @JsonCreator
    public static TstpWriteFormat from(String value) {
        return switch (value) {
            case "binary" -> BINARY;
            case "ascii" -> ASCII;
            default -> throw new IllegalArgumentException("tstp.server.writeFormat must be binary or ascii");
        };
    }
}
