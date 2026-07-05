package at.pegelhub.shared.duration;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PegelhubDurationLiteralTest {

    @Test
    void acceptsPositiveDurations() {
        assertThat(new PegelhubDurationLiteral("72h").toString()).isEqualTo("72h");
        assertThat(new PegelhubDurationLiteral(" 1h30m ").toString()).isEqualTo("1h30m");
        assertThat(new PegelhubDurationLiteral("2w").toDuration()).isEqualTo(Duration.ofDays(14));
    }

    @Test
    void parsesCompositeDurations() {
        assertThat(new PegelhubDurationLiteral("1h30m").toDuration()).isEqualTo(Duration.ofMinutes(90));
        assertThat(new PegelhubDurationLiteral("1d6h").toDuration()).isEqualTo(Duration.ofHours(30));
    }

    @Test
    void formatsJavaDurationsAsPegelhubDurations() {
        assertThat(PegelhubDurationLiteral.from(Duration.ofSeconds(30)).toString()).isEqualTo("30s");
        assertThat(PegelhubDurationLiteral.from(Duration.ofMinutes(15)).toString()).isEqualTo("15m");
        assertThat(PegelhubDurationLiteral.from(Duration.ofHours(6)).toString()).isEqualTo("6h");
        assertThat(PegelhubDurationLiteral.from(Duration.ofDays(7)).toString()).isEqualTo("7d");
    }

    @Test
    void rejectsBlankNegativeSubSecondTooLargeAndArbitraryInput() {
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral(null));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral(""));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("-3d"));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("500ms"));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("250us"));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("1mo"));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("1y"));
        assertThrows(IllegalArgumentException.class, () -> new PegelhubDurationLiteral("null"));
        assertThrows(IllegalArgumentException.class, () ->
                new PegelhubDurationLiteral("1h) |> drop(columns: [\"_value\"])"));
        assertThrows(IllegalArgumentException.class, () -> PegelhubDurationLiteral.from(Duration.ofMillis(500)));
    }
}
