package at.pegelhub.timeseries.api;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.CreateTimeSeriesCommand;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.UnitCode;

import java.time.Duration;

final class TimeSeriesMapper {

    private TimeSeriesMapper() {
    }

    static CreateTimeSeriesCommand toCommand(CreateTimeSeriesRequest request) {
        return new CreateTimeSeriesCommand(
                new StationId(request.stationId()),
                new ObservedPropertyCode(request.observedProperty()),
                new UnitCode(request.unit()),
                request.referenceLevel(),
                toDuration(request.expectedIntervalSeconds()),
                toExternalCode(request.externalCode()));
    }

    static TimeSeriesResponse toResponse(TimeSeries timeSeries) {
        return new TimeSeriesResponse(
                timeSeries.id().value(),
                timeSeries.stationId().value(),
                timeSeries.observedProperty().value(),
                timeSeries.unit().value(),
                timeSeries.referenceLevel(),
                toSeconds(timeSeries.expectedInterval()),
                toExternalCodeValue(timeSeries.externalCode()));
    }

    private static Duration toDuration(Long seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }

    private static ExternalTimeSeriesCode toExternalCode(String externalCode) {
        return externalCode == null || externalCode.isBlank() ? null : new ExternalTimeSeriesCode(externalCode);
    }

    private static Long toSeconds(Duration duration) {
        return duration == null ? null : duration.toSeconds();
    }

    private static String toExternalCodeValue(ExternalTimeSeriesCode externalCode) {
        return externalCode == null ? null : externalCode.value();
    }
}
