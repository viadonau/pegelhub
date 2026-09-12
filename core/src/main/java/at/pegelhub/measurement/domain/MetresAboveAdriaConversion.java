package at.pegelhub.measurement.domain;

import java.math.BigDecimal;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;

/**
 * Converts absolute water levels to and from centimetres above gauge zero.
 * Use the measuring point's current gauge zero, not its value at observation time.
 * If the gauge zero changes, later reads of old measurements in metres above Adria will differ.
 *
 * <p>Decimal arithmetic avoids floating-point subtraction errors around gauge zero.
 * For example, {@code 155.56} metres above Adria at gauge zero {@code 152.68} gives exactly
 * {@code 288} cm. Results are still doubles, with no extra rounding.
 *
 * @param gaugeZeroElevationMAboveAdria the required gauge zero, held fixed for both directions
 */
public record MetresAboveAdriaConversion(
        BigDecimal gaugeZeroElevationMAboveAdria) implements MeasurementConversion {

    public MetresAboveAdriaConversion {
        if (gaugeZeroElevationMAboveAdria == null) {
            throw new IllegalArgumentException(
                    "Metres above Adria requires a measuring point gauge zero elevation");
        }
    }

    @Override
    public String unit() {
        return "m";
    }

    @Override
    public double toCanonical(double value) {
        double result = BigDecimal.valueOf(requireFinite(value))
                .subtract(gaugeZeroElevationMAboveAdria)
                .movePointRight(2)
                .doubleValue();
        return requireFinite(result);
    }

    @Override
    public double fromCanonical(double value) {
        double result = BigDecimal.valueOf(requireFinite(value))
                .movePointLeft(2)
                .add(gaugeZeroElevationMAboveAdria)
                .doubleValue();
        return requireFinite(result);
    }
}
