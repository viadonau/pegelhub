package at.pegelhub.measurement.domain;

import java.math.BigDecimal;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;

/** Converts discharge between litres per second and storage in cubic metres per second. */
public final class LitresPerSecondConversion implements MeasurementConversion {

    @Override
    public String unit() {
        return "l/s";
    }

    @Override
    public double toCanonical(double value) {
        double result = BigDecimal.valueOf(requireFinite(value)).movePointLeft(3).doubleValue();
        return requireFinite(result);
    }

    @Override
    public double fromCanonical(double value) {
        double result = BigDecimal.valueOf(requireFinite(value)).movePointRight(3).doubleValue();
        return requireFinite(result);
    }
}
