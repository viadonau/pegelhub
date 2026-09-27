package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.api.read.output.MeasurementIntervalListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementPointResponse;
import at.pegelhub.measurement.api.read.output.MeasurementSortOrder;
import at.pegelhub.measurement.api.read.output.MeasurementWindowResponse;
import at.pegelhub.measurement.application.MeasurementInterval;
import at.pegelhub.measurement.application.MeasurementIntervalList;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;

public final class MeasurementReadResponseMapper {

    private MeasurementReadResponseMapper() {
    }

    public static MeasurementListResponse toResponse(MeasurementList list) {
        return new MeasurementListResponse(
                list.query().timeSeriesId().value(),
                toWindowResponse(list.query().window()),
                toResponseOrder(list.query().order()),
                list.query().limit(),
                list.truncated(),
                list.measurements().stream()
                        .map(MeasurementReadResponseMapper::toPointResponse)
                        .toList(),
                list.query().representation(),
                list.unit());
    }

    public static MeasurementIntervalListResponse toResponse(MeasurementIntervalList list) {
        var query = list.query();
        return new MeasurementIntervalListResponse(
                query.timeSeriesId().value(), query.from(), query.to(), query.interval(), query.timeBasis(),
                query.closedOnly(), query.representation(), list.unit(), "time-weighted-step",
                list.computedAt(), list.intervals().stream()
                        .map(MeasurementReadResponseMapper::toIntervalResponse)
                        .toList());
    }

    private static MeasurementWindowResponse toWindowResponse(MeasurementWindow window) {
        return new MeasurementWindowResponse(window.from(), window.to(), window.requested());
    }

    private static MeasurementPointResponse toPointResponse(MeasurementReadRow measurement) {
        return new MeasurementPointResponse(
                measurement.observedAt(),
                measurement.value());
    }

    private static MeasurementIntervalListResponse.IntervalResponse toIntervalResponse(
            MeasurementInterval interval) {
        return new MeasurementIntervalListResponse.IntervalResponse(
                interval.from(), interval.to(), interval.mean(), interval.observationCount(),
                interval.supportedNanos(), interval.lastContributingObservedAt(),
                interval.windowStatus(), interval.supportStatus());
    }

    private static MeasurementSortOrder toResponseOrder(MeasurementOrder order) {
        return switch (order) {
            case ASC -> MeasurementSortOrder.ASC;
            case DESC -> MeasurementSortOrder.DESC;
        };
    }

}
