package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static at.pegelhub.timeseries.domain.MeasurementRepresentation.CANONICAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasurementIntervalQueryTest {
    private static final TimeSeriesId ID = new TimeSeriesId(UUID.randomUUID());

    @Test
    void fixedMezDaysStayAtUtc23OnBothDstTransitionDates() {
        for (String date : new String[]{"2026-03-28T23:00:00Z", "2026-10-24T23:00:00Z"}) {
            Instant from = Instant.parse(date);
            var query = new MeasurementIntervalQuery(ID, from, from.plusSeconds(86_400),
                    "1d", "+01:00", true, CANONICAL);
            assertThat(query.width().getSeconds()).isEqualTo(86_400);
            assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from.plusSeconds(3_600),
                    from.plusSeconds(90_000), "1d", "+01:00", true, CANONICAL))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void requiresFullAlignedWindowsAndRejectsOversize() {
        Instant from = Instant.parse("2026-06-17T00:00:00Z");
        assertThatCode(() -> new MeasurementIntervalQuery(ID, from, from.plusSeconds(50_000L * 900),
                "15m", "UTC", true, CANONICAL)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from.plusSeconds(1),
                from.plusSeconds(901), "15m", "UTC", true, CANONICAL))
                .hasMessageContaining("aligned");
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from,
                from.plusSeconds(50_001L * 900), "15m", "UTC", true, CANONICAL))
                .hasMessageContaining("50000");
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from,
                from.plusSeconds(900), "14m", "UTC", true, CANONICAL))
                .hasMessageContaining("interval");
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from,
                from.plusSeconds(900), null, "UTC", true, CANONICAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interval");
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from,
                from.plusSeconds(900), "15m", "Europe/Vienna", true, CANONICAL))
                .hasMessageContaining("timeBasis");
        assertThatThrownBy(() -> new MeasurementIntervalQuery(ID, from,
                from.plusSeconds(900), "15m", null, true, CANONICAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeBasis");
    }
}
