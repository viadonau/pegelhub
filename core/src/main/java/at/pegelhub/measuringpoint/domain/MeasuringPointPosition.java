package at.pegelhub.measuringpoint.domain;

import java.math.BigDecimal;

public record MeasuringPointPosition(
        BigDecimal riverKilometer,
        BankSide bank,
        Coordinates coordinates) {

    public MeasuringPointPosition {
        if (riverKilometer != null && riverKilometer.signum() < 0) {
            throw new IllegalArgumentException("riverKilometer must not be negative");
        }
    }

    public static MeasuringPointPosition empty() {
        return new MeasuringPointPosition(null, null, null);
    }

    public boolean isEmpty() {
        return riverKilometer == null && bank == null && coordinates == null;
    }
}
