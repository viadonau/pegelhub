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
    void sourceRepresentationsUseStableWireValues() {
        assertThat(SourceRepresentation.CANONICAL.value()).isEqualTo("canonical");
        assertThat(SourceRepresentation.METRES_ABOVE_ADRIA.value()).isEqualTo("metres-above-adria");
        assertThat(SourceRepresentation.from(" METRES-ABOVE-ADRIA "))
                .isEqualTo(SourceRepresentation.METRES_ABOVE_ADRIA);
        assertThrows(IllegalArgumentException.class, () -> SourceRepresentation.from("absolute"));
    }

    @Test
    void keepsTimeSeriesIdValue() {
        UUID id = UUID.fromString("75ad6d22-f98f-47bd-8238-1c308c4cfda8");
        assertThat(new TimeSeriesId(id).value()).isEqualTo(id);
    }
}
