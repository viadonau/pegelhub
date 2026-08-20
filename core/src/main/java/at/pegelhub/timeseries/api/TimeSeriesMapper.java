package at.pegelhub.timeseries.api;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.timeseries.application.CreateTimeSeriesCommand;
import at.pegelhub.timeseries.application.UpdateTimeSeriesCommand;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.TimeSeries;

final class TimeSeriesMapper {
    private TimeSeriesMapper() { }

    static CreateTimeSeriesCommand toCommand(CreateTimeSeriesRequest request) {
        return new CreateTimeSeriesCommand(
                new MeasuringPointId(request.measuringPointId()), new ObservedPropertyCode(request.observedProperty()),
                request.status(), assignment(request.sourceAssignment()));
    }

    static UpdateTimeSeriesCommand toCommand(UpdateTimeSeriesRequest request) {
        return new UpdateTimeSeriesCommand(request.status(), assignment(request.sourceAssignment()));
    }

    static TimeSeriesResponse toResponse(TimeSeries series) {
        var assignment = series.sourceAssignment();
        return new TimeSeriesResponse(
                series.id().value(), series.measuringPointId().value(), series.observedProperty().value(),
                series.unit(), series.status(), assignment == null ? null
                        : new TimeSeriesResponse.SourceAssignmentResponse(assignment.connectorId().value(), assignment.representation()));
    }

    private static SourceAssignment assignment(SourceAssignmentRequest request) {
        return request == null ? null : new SourceAssignment(new ConnectorId(request.connectorId()), request.representation());
    }
}
