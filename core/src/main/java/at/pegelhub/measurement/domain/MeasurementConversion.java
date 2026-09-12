package at.pegelhub.measurement.domain;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.ObservedPropertyCatalog;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

/**
 * One metadata snapshot for converting between a wire representation and canonical storage.
 */
public record MeasurementConversion(
        ObservedPropertyCode observedProperty,
        MeasurementRepresentation representation,
        BigDecimal gaugeZeroElevationMAboveAdria) {

    public MeasurementConversion {
        requireNonNull(observedProperty);
        requireNonNull(representation);
        if (!ObservedPropertyCatalog.allows(observedProperty.value(), representation)) {
            throw new IllegalArgumentException(
                    "Representation " + representation.value()
                            + " is not supported for " + observedProperty.value());
        }
        if (representation == MeasurementRepresentation.METRES_ABOVE_ADRIA
                && gaugeZeroElevationMAboveAdria == null) {
            throw new IllegalArgumentException(
                    "Metres above Adria requires a measuring point gauge zero elevation");
        }
    }

    public String unit() {
        return switch (representation) {
            case CANONICAL -> observedProperty.definition().canonicalUnit();
            case METRES_ABOVE_ADRIA -> "m";
            case LITRES_PER_SECOND -> "l/s";
        };
    }

    public double toCanonical(double value) {
        BigDecimal input = BigDecimal.valueOf(finite(value));
        return finite(
                switch (representation) {
                    case CANONICAL -> value;
                    case METRES_ABOVE_ADRIA -> input
                            .subtract(gaugeZeroElevationMAboveAdria)
                            .movePointRight(2)
                            .doubleValue();
                    case LITRES_PER_SECOND -> input.movePointLeft(3).doubleValue();
                });
    }

    public double fromCanonical(double value) {
        BigDecimal input = BigDecimal.valueOf(finite(value));
        return finite(
                switch (representation) {
                    case CANONICAL -> value;
                    case METRES_ABOVE_ADRIA -> input
                            .movePointLeft(2)
                            .add(gaugeZeroElevationMAboveAdria)
                            .doubleValue();
                    case LITRES_PER_SECOND -> input.movePointRight(3).doubleValue();
                });
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Measurement conversion requires a finite value and result");
        }
        return value;
    }
}
