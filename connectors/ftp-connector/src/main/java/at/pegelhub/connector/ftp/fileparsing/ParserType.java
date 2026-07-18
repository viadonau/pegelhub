package at.pegelhub.connector.ftp.fileparsing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ParserType {
    ASC("asc", ".asc"),
    ZRXP("zrxp", ".zrxp");

    private final String value;
    public final String fileSuffix;

    ParserType(String value, String fileSuffix) {
        this.value = value;
        this.fileSuffix = fileSuffix;
    }

    @JsonCreator
    public static ParserType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown FTP parser type: " + value));
    }

    @JsonValue
    public String value() {
        return value;
    }
}
