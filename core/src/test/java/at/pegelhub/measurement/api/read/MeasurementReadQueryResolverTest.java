package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.api.read.input.MeasurementReadParameters;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class MeasurementReadQueryResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-17T13:00:00Z"), ZoneOffset.UTC);
    private static final UUID TIME_SERIES_ID = UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76");
    private final MeasurementReadQueryResolver resolver = new MeasurementReadQueryResolver(CLOCK);

    @Test
    void resolvesRawReadParametersIntoAStableWindow() {
        var query = resolver.resolveList(TIME_SERIES_ID, new MeasurementReadParameters(
                "24h",
                null,
                null,
                "desc",
                100));

        assertThat(query.window().from()).isEqualTo(Instant.parse("2026-06-16T13:00:00Z"));
        assertThat(query.window().to()).isEqualTo(Instant.parse("2026-06-17T13:00:00Z"));
        assertThat(query.window().requested()).isEqualTo("24h");
        assertThat(query.order()).isEqualTo(MeasurementOrder.DESC);
        assertThat(query.limit()).isEqualTo(100);
    }

}
