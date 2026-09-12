package at.pegelhub.measurement.domain;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static at.pegelhub.timeseries.domain.MeasurementRepresentation.CANONICAL;
import static at.pegelhub.timeseries.domain.MeasurementRepresentation.LITRES_PER_SECOND;
import static at.pegelhub.timeseries.domain.MeasurementRepresentation.METRES_ABOVE_ADRIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MeasurementConversionTest {
    @Test
    void litresPerSecondRoundTripKeepsCanonicalCubicMetresPerSecond() {
        var conversion = conversion("discharge", LITRES_PER_SECOND, null);
        assertThat(conversion.toCanonical(1250.5)).isEqualTo(1.2505);
        assertThat(conversion.fromCanonical(1.2505)).isEqualTo(1250.5);
        assertThat(conversion.toCanonical(0)).isZero();
        assertThat(conversion.fromCanonical(-0.001)).isEqualTo(-1);
        assertThat(conversion.unit()).isEqualTo("l/s");
    }

    @Test
    void absoluteWaterLevelUsesOneGaugeZeroInBothDirections() {
        var conversion = conversion(
                "water-level", METRES_ABOVE_ADRIA, new BigDecimal("152.68"));
        assertThat(conversion.toCanonical(155.56)).isEqualTo(288);
        assertThat(conversion.fromCanonical(288)).isEqualTo(155.56);
        assertThat(conversion.fromCanonical(-12.5)).isEqualTo(152.555);
        assertThat(conversion.unit()).isEqualTo("m");
    }

    @Test
    void canonicalValuesAndUnitsAreUnchanged() {
        for (String property : new String[]{"water-level", "water-temperature", "discharge"}) {
            var conversion = conversion(property, CANONICAL, null);
            assertThat(conversion.toCanonical(12.5)).isEqualTo(12.5);
            assertThat(conversion.fromCanonical(12.5)).isEqualTo(12.5);
            assertThat(conversion.unit()).isEqualTo(
                    new ObservedPropertyCode(property).definition().canonicalUnit());
        }
    }

    @Test
    void incompatibleRepresentationsAndMissingGaugeZeroAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion("water-level", LITRES_PER_SECOND, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion("discharge", METRES_ABOVE_ADRIA, BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion("water-temperature", LITRES_PER_SECOND, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion("water-level", METRES_ABOVE_ADRIA, null));
    }

    @Test
    void nonFiniteInputsAndConversionOverflowAreRejected() {
        var conversion = conversion("discharge", LITRES_PER_SECOND, null);
        assertThatIllegalArgumentException().isThrownBy(() -> conversion.toCanonical(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion.fromCanonical(Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(
                () -> conversion.fromCanonical(Double.MAX_VALUE));
    }

    private static MeasurementConversion conversion(
            String property,
            MeasurementRepresentation representation,
            BigDecimal gaugeZero) {
        return new MeasurementConversion(
                new ObservedPropertyCode(property), representation, gaugeZero);
    }
}
