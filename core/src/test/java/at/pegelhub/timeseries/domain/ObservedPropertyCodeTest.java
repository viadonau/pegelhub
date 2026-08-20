package at.pegelhub.timeseries.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ObservedPropertyCodeTest {

    @Test
    void canonicalizesKnownAliasesAndDerivesTheirUnits() {
        assertThat(new ObservedPropertyCode(" W ").value()).isEqualTo("water-level");
        assertThat(new ObservedPropertyCode("water_level").value()).isEqualTo("water-level");
        assertThat(new ObservedPropertyCode("Wasser Stand").value()).isEqualTo("water-level");
        assertThat(new ObservedPropertyCode("Wassertemperatur").value()).isEqualTo("water-temperature");
        assertThat(new ObservedPropertyCode("Wasser Temperatur").value()).isEqualTo("water-temperature");
        assertThat(new ObservedPropertyCode("Q").value()).isEqualTo("discharge");
        assertThat(new ObservedPropertyCode(" Abfluss ").value()).isEqualTo("discharge");
        assertThat(new ObservedPropertyCode("water-level").definition().canonicalUnit()).isEqualTo("cm");
        assertThat(new ObservedPropertyCode("water-temperature").definition().canonicalUnit()).isEqualTo("Cel");
        assertThat(new ObservedPropertyCode("discharge").definition().canonicalUnit()).isEqualTo("m3/s");
    }

    @Test
    void preservesUnknownValuesAndRejectsBlankValues() {
        assertThrows(NullPointerException.class, () -> new ObservedPropertyCode(null));
        assertThrows(IllegalArgumentException.class, () -> new ObservedPropertyCode(" "));
        assertThat(new ObservedPropertyCode(" level ").value()).isEqualTo("level");
        assertThat(new ObservedPropertyCode(" Air temperature ").value()).isEqualTo("Air temperature");
    }
}
