package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementIntervalQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.api.read.input.MeasurementReadParameters;
import at.pegelhub.measurement.api.read.input.MeasurementIntervalParameters;
import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
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
    private final Clock clock;

    public MeasurementReadQueryResolver(Clock clock) {
        this.clock = requireNonNull(clock);
    }

    public MeasurementListQuery resolveList(UUID timeSeriesId, MeasurementReadParameters parameters) {
        requireNonNull(timeSeriesId);
        requireNonNull(parameters);
        return new MeasurementListQuery(
                new TimeSeriesId(timeSeriesId),
                window(parameters.last(), parameters.from(), parameters.to()),
                order(parameters.order()),
                parameters.limit() == null ? DEFAULT_LIMIT : parameters.limit(),
                representation(parameters.representation()));
    }

    public MeasurementIntervalQuery resolveIntervals(UUID timeSeriesId, MeasurementIntervalParameters parameters) {
        requireNonNull(timeSeriesId);
        requireNonNull(parameters);
        if (parameters.from() == null || parameters.to() == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        return new MeasurementIntervalQuery(new TimeSeriesId(timeSeriesId), parameters.from(), parameters.to(),
                parameters.interval(), parameters.timeBasis() == null ? "UTC" : parameters.timeBasis(),
                parameters.closedOnly() == null || parameters.closedOnly(), representation(parameters.representation()));
    }

    private static MeasurementRepresentation representation(String value) {
        return value == null ? MeasurementRepresentation.CANONICAL : MeasurementRepresentation.from(value);
    }

    private MeasurementWindow window(String last, Instant from, Instant to) {
        String relativeWindow = blankToNull(last);
        boolean hasLast = relativeWindow != null;
        boolean hasExplicitWindow = from != null || to != null;
        if (hasLast == hasExplicitWindow) {
            throw new IllegalArgumentException("Provide either last or from/to");
        }
        if (hasLast) {
            PegelhubDurationLiteral duration = new PegelhubDurationLiteral(relativeWindow);
            Instant resolvedTo = Instant.now(clock);
            return new MeasurementWindow(
                    resolvedTo.minus(duration.toDuration()),
                    resolvedTo,
                    duration.toString());
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
