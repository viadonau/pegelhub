package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementBucketResolution;
import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementBucketWidth;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.api.read.input.MeasurementBucketParameters;
import at.pegelhub.measurement.api.read.input.MeasurementPageParameters;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Resolves HTTP Measurement read parameters into fully typed application queries.
 */
@Component
public class MeasurementReadQueryResolver {

    private static final int DEFAULT_LIMIT = 1_000;
    private static final int DEFAULT_MAX_POINTS = 500;

    private final Clock clock;
    private final MeasurementBucketResolutionPolicy bucketResolutionPolicy;
    private final MeasurementCursorCodec cursorCodec;

    public MeasurementReadQueryResolver(Clock clock, MeasurementBucketResolutionPolicy bucketResolutionPolicy) {
        this.clock = requireNonNull(clock);
        this.bucketResolutionPolicy = requireNonNull(bucketResolutionPolicy);
        this.cursorCodec = new MeasurementCursorCodec();
    }

    public MeasurementListQuery resolvePage(UUID timeSeriesId, MeasurementPageParameters parameters) {
        requireNonNull(timeSeriesId);
        requireNonNull(parameters);
        return new MeasurementListQuery(
                new TimeSeriesId(timeSeriesId),
                window(parameters.last(), parameters.from(), parameters.to()),
                order(parameters.order()),
                parameters.limit() == null ? DEFAULT_LIMIT : parameters.limit(),
                cursorCodec.decode(parameters.cursor()));
    }

    public MeasurementBucketQuery resolveBuckets(UUID timeSeriesId, MeasurementBucketParameters parameters) {
        requireNonNull(timeSeriesId);
        requireNonNull(parameters);
        MeasurementWindow window = window(parameters.last(), parameters.from(), parameters.to());
        String bucket = blankToNull(parameters.bucket());
        if (bucket != null && parameters.maxPoints() != null) {
            throw new IllegalArgumentException("Provide either bucket or maxPoints");
        }
        MeasurementBucketResolution resolution = bucket == null
                ? bucketResolutionPolicy.automatic(window, parameters.maxPoints() == null ? DEFAULT_MAX_POINTS : parameters.maxPoints())
                : MeasurementBucketResolution.explicit(new MeasurementBucketWidth(new FluxDuration(bucket).toDuration()));
        return new MeasurementBucketQuery(new TimeSeriesId(timeSeriesId), window, resolution);
    }

    private MeasurementWindow window(String last, Instant from, Instant to) {
        String relativeWindow = blankToNull(last);
        boolean hasLast = relativeWindow != null;
        boolean hasExplicitWindow = from != null || to != null;
        if (hasLast == hasExplicitWindow) {
            throw new IllegalArgumentException("Provide either last or from/to");
        }
        if (hasLast) {
            FluxDuration duration = new FluxDuration(relativeWindow);
            Instant resolvedTo = Instant.now(clock);
            return new MeasurementWindow(resolvedTo.minus(duration.toDuration()), resolvedTo, duration.toString());
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to are required");
        }
        return new MeasurementWindow(from, to, null);
    }

    private static MeasurementOrder order(String value) {
        if (value == null || value.isBlank()) {
            return MeasurementOrder.ASC;
        }
        return switch (value.trim().toLowerCase()) {
            case "asc" -> MeasurementOrder.ASC;
            case "desc" -> MeasurementOrder.DESC;
            default -> throw new IllegalArgumentException("order must be asc or desc");
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
