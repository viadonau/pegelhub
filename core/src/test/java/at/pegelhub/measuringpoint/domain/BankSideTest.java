package at.pegelhub.measuringpoint.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BankSideTest {

    @Test
    void mapsCanonicalValues() {
        assertThat(BankSide.fromNullable("left")).isEqualTo(BankSide.LEFT);
        assertThat(BankSide.fromNullable("right")).isEqualTo(BankSide.RIGHT);
        assertThat(BankSide.fromNullable(" left ")).isEqualTo(BankSide.LEFT);
    }

    @Test
    void mapsMissingOptionalValuesToNull() {
        assertThat(BankSide.fromNullable(" ")).isNull();
        assertThat(BankSide.fromNullable(null)).isNull();
    }

    @Test
    void rejectsNonCanonicalValues() {
        assertThrows(IllegalArgumentException.class, () -> BankSide.fromNullable("R"));
        assertThrows(IllegalArgumentException.class, () -> BankSide.fromNullable("rechts"));
        assertThrows(IllegalArgumentException.class, () -> BankSide.fromNullable("north"));
    }
}
