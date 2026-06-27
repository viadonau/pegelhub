package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.api.read.input.MeasurementBucketParameters;
import at.pegelhub.measurement.api.read.input.MeasurementPageParameters;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class MeasurementReadQueryResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-17T13:00:00Z"), ZoneOffset.UTC);
    private static final UUID TIME_SERIES_ID = UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76");
    private final MeasurementReadQueryResolver resolver = new MeasurementReadQueryResolver(
            CLOCK,
            new MeasurementBucketResolutionPolicy());

    @Test
    void resolvesRawPageParametersIntoAStableWindow() {
        var query = resolver.resolvePage(TIME_SERIES_ID, new MeasurementPageParameters(
                "24h",
                null,
                null,
                "desc",
                100,
                null));

        assertThat(query.window().from()).isEqualTo(Instant.parse("2026-06-16T13:00:00Z"));
        assertThat(query.window().to()).isEqualTo(Instant.parse("2026-06-17T13:00:00Z"));
        assertThat(query.order()).isEqualTo(MeasurementOrder.DESC);
        assertThat(query.limit()).isEqualTo(100);
    }

    @Test
    void resolvesExplicitBucketWithoutAnAutomaticTarget() {
        var query = resolver.resolveBuckets(TIME_SERIES_ID, new MeasurementBucketParameters(
                "24h",
                null,
                null,
                "5m",
                null));

        assertThat(query.resolution().bucketWidth().toString()).isEqualTo("5m");
        assertThat(query.resolution().targetPointCount()).isNull();
    }

    @Test
    void rejectsAnAutomaticResolutionThatCannotHonorThePointCap() {
        assertThatThrownBy(() -> resolver.resolveBuckets(TIME_SERIES_ID, new MeasurementBucketParameters(
                "100y",
                null,
                null,
                null,
                1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be resolved within maxPoints");
    }
}
