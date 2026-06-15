package at.pegelhub.timeseries.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSeriesValueObjectsTest {

    @Test
    void timeSeriesIdRejectsMissingValue() {
        assertThrows(NullPointerException.class, () -> new TimeSeriesId(null));
    }

    @Test
    void observedPropertyCodeRejectsBlankValue() {
        assertThrows(NullPointerException.class, () -> new ObservedPropertyCode(null));
        assertThrows(IllegalArgumentException.class, () -> new ObservedPropertyCode(" "));
    }

    @Test
    void unitCodeRejectsBlankValue() {
        assertThrows(NullPointerException.class, () -> new UnitCode(null));
        assertThrows(IllegalArgumentException.class, () -> new UnitCode(" "));
    }

    @Test
    void externalTimeSeriesCodeRejectsBlankValue() {
        assertThrows(NullPointerException.class, () -> new ExternalTimeSeriesCode(null));
        assertThrows(IllegalArgumentException.class, () -> new ExternalTimeSeriesCode(" "));
    }

    @Test
    void trimsCodeValues() {
        assertThat(new ObservedPropertyCode(" water-level ").value()).isEqualTo("water-level");
        assertThat(new UnitCode(" cm ").value()).isEqualTo("cm");
        assertThat(new ExternalTimeSeriesCode(" stage-main ").value()).isEqualTo("stage-main");
    }

    @Test
    void keepsTimeSeriesIdValue() {
        var id = UUID.fromString("75ad6d22-f98f-47bd-8238-1c308c4cfda8");

        assertThat(new TimeSeriesId(id).value()).isEqualTo(id);
    }
}
