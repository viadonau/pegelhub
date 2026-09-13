package at.pegelhub.measurement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MeasurementConversionTest {

    @Test
    void litresPerSecondRoundTripKeepsCanonicalCubicMetresPerSecond() {
        var conversion = new LitresPerSecondConversion();

        assertThat(conversion.toCanonical(1250.5)).isEqualTo(1.2505);
        assertThat(conversion.fromCanonical(1.2505)).isEqualTo(1250.5);
        assertThat(conversion.toCanonical(0)).isZero();
        assertThat(conversion.fromCanonical(0)).isZero();
        assertThat(conversion.toCanonical(-1)).isEqualTo(-0.001);
        assertThat(conversion.fromCanonical(-0.001)).isEqualTo(-1);
        assertThat(conversion.unit()).isEqualTo("l/s");
    }

    @Test
    void absoluteWaterLevelUsesOneGaugeZeroInBothDirections() {
        var conversion = new MetresAboveAdriaConversion(new BigDecimal("152.68"));

        assertThat(conversion.toCanonical(155.56)).isEqualTo(288);
        assertThat(conversion.fromCanonical(288)).isEqualTo(155.56);
        assertThat(conversion.toCanonical(152.555)).isEqualTo(-12.5);
        assertThat(conversion.fromCanonical(-12.5)).isEqualTo(152.555);
        assertThat(conversion.toCanonical(152.68)).isZero();
        assertThat(conversion.fromCanonical(0)).isEqualTo(152.68);
        assertThat(conversion.unit()).isEqualTo("m");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cm", "Cel", "m3/s"})
    void canonicalValuesAndUnitsAreUnchanged(String unit) {
        var conversion = new CanonicalConversion(unit);

        for (double value : new double[]{12.5, -12.5, 0.0, -0.0, Double.MIN_VALUE, Double.MAX_VALUE}) {
            assertThat(conversion.toCanonical(value)).isEqualTo(value);
            assertThat(conversion.fromCanonical(value)).isEqualTo(value);
        }
        assertThat(conversion.unit()).isEqualTo(unit);
    }

    @Test
    void absoluteWaterLevelRequiresGaugeZeroBeforeAnyValuesAreConverted() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MetresAboveAdriaConversion(null))
                .withMessageContaining("gauge zero");
    }

    @ParameterizedTest
    @MethodSource("conversions")
    void rejectsNonFiniteInputsInBothDirections(MeasurementConversion conversion) {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThatIllegalArgumentException().isThrownBy(() -> conversion.toCanonical(value));
            assertThatIllegalArgumentException().isThrownBy(() -> conversion.fromCanonical(value));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.MAX_VALUE, -Double.MAX_VALUE})
    void rejectsOverflowWhenScalingUp(double value) {
        var litres = new LitresPerSecondConversion();
        var absolute = new MetresAboveAdriaConversion(BigDecimal.ZERO);

        assertThatIllegalArgumentException().isThrownBy(() -> litres.fromCanonical(value));
        assertThatIllegalArgumentException().isThrownBy(() -> absolute.toCanonical(value));
    }

    @Test
    void rejectsOverflowWhenAddingGaugeZero() {
        var conversion = new MetresAboveAdriaConversion(BigDecimal.valueOf(Double.MAX_VALUE));

        assertThatIllegalArgumentException().isThrownBy(() -> conversion.fromCanonical(Double.MAX_VALUE));
    }

    private static Stream<MeasurementConversion> conversions() {
        return Stream.of(
                new CanonicalConversion("cm"),
                new LitresPerSecondConversion(),
                new MetresAboveAdriaConversion(new BigDecimal("152.68")));
    }
}
