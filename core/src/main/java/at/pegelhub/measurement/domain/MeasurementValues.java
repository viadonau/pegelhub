package at.pegelhub.measurement.domain;

/** Value checks shared by measurement records and conversions. */
public final class MeasurementValues {

    private MeasurementValues() {
    }

    /** Rejects NaN and infinity without changing finite values, including signed zero. */
    public static double requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        return value;
    }
}
