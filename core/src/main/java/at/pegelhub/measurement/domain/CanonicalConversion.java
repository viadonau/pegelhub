package at.pegelhub.measurement.domain;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

/** Leaves values in storage units unchanged. */
public record CanonicalConversion(String unit) implements MeasurementConversion {

    public CanonicalConversion {
        requireNonNull(unit);
    }

    @Override
    public double toCanonical(double value) {
        return requireFinite(value);
    }

    @Override
    public double fromCanonical(double value) {
        return toCanonical(value);
    }
}
