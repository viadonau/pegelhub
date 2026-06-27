package at.pegelhub.shared.influx;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FluxDurationTest {

    @Test
    void acceptsPositiveFluxDurations() {
        assertThat(new FluxDuration("72h").negativeLiteral()).isEqualTo("-72h");
        assertThat(new FluxDuration(" 1h30m ").toString()).isEqualTo("1h30m");
        assertThat(new FluxDuration("500ms").toString()).isEqualTo("500ms");
    }

    @Test
    void convertsFluxDurationsToJavaDurations() {
        assertThat(new FluxDuration("1h30m").toDuration()).isEqualTo(Duration.ofMinutes(90));
        assertThat(new FluxDuration("2w").toDuration()).isEqualTo(Duration.ofDays(14));
        assertThat(new FluxDuration("500ms").toDuration()).isEqualTo(Duration.ofMillis(500));
        assertThat(new FluxDuration("250us").toDuration()).isEqualTo(Duration.ofNanos(250_000));
    }

    @Test
    void rejectsBlankNegativeAndArbitraryInput() {
        assertThrows(IllegalArgumentException.class, () -> new FluxDuration(null));
        assertThrows(IllegalArgumentException.class, () -> new FluxDuration(""));
        assertThrows(IllegalArgumentException.class, () -> new FluxDuration("-3d"));
        assertThrows(IllegalArgumentException.class, () -> new FluxDuration("null"));
        assertThrows(IllegalArgumentException.class, () -> new FluxDuration("1h) |> drop(columns: [\"_value\"])"));
    }
}
