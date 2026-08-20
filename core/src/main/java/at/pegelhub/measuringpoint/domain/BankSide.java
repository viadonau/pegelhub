package at.pegelhub.measuringpoint.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Canonical bank values used by metadata and monitoring read models. */
public enum BankSide {
    LEFT("left"),
    RIGHT("right");

    private final String value;

    BankSide(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BankSide from(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Bank side must not be null");
        }
        BankSide side = fromNullable(input);
        if (side == null) {
            throw new IllegalArgumentException("Bank side must not be blank");
        }
        return side;
    }
    public static BankSide fromNullable(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isBlank()) {
            return null;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "left" -> LEFT;
            case "right" -> RIGHT;
            default -> throw new IllegalArgumentException("Unknown bank side: " + input);
        };
    }
}
