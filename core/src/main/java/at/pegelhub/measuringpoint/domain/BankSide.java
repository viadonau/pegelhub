package at.pegelhub.measuringpoint.domain;

/** Canonical bank values used by metadata and monitoring read models. */
public enum BankSide {
    LEFT("left"),
    RIGHT("right");

    private final String value;

    BankSide(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static BankSide fromNullable(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "left" -> LEFT;
            case "right" -> RIGHT;
            default -> throw new IllegalArgumentException("Unknown bank side: " + input);
        };
    }
}
