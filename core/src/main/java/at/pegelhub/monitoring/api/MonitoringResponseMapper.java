package at.pegelhub.monitoring.api;

import at.pegelhub.monitoring.application.MonitoringCollection;
import at.pegelhub.monitoring.application.MonitoringDetail;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.station.domain.Station;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.timeseries.domain.TimeSeries;

public final class MonitoringResponseMapper {

    private MonitoringResponseMapper() {
    }

    public static MonitoringResponse.TimeSeriesCollection toResponse(MonitoringCollection collection) {
        return new MonitoringResponse.TimeSeriesCollection(
                collection.items().stream().map(item -> new MonitoringResponse.TimeSeriesSummary(
                        item.timeSeries().id().value(),
                        item.timeSeries().observedProperty().value(),
                        item.timeSeries().unit().value(),
                        toSummary(item.measuringPoint()),
                        toSummary(item.station()),
                        toLatest(item.latestMeasurement()))).toList());
    }

    public static MonitoringResponse.TimeSeriesDetail toResponse(MonitoringDetail detail) {
        TimeSeries timeSeries = detail.timeSeries();
        return new MonitoringResponse.TimeSeriesDetail(
                timeSeries.id().value(),
                timeSeries.observedProperty().value(),
                timeSeries.unit().value(),
                timeSeries.externalCode() == null ? null : timeSeries.externalCode().value(),
                toMeasuringPoint(detail.measuringPoint()),
                toSummary(detail.station()),
                toOwner(detail.stationOwner()),
                toLatest(detail.latestMeasurement()));
    }

    private static MonitoringResponse.MeasuringPointSummary toSummary(MeasuringPoint point) {
        return new MonitoringResponse.MeasuringPointSummary(point.id().value(), point.name());
    }

    private static MonitoringResponse.MeasuringPoint toMeasuringPoint(MeasuringPoint point) {
        return new MonitoringResponse.MeasuringPoint(
                point.id().value(),
                point.name(),
                point.referenceLevel(),
                point.referenceYear(),
                point.riverKilometer(),
                point.bank() == null ? null : point.bank().value(),
                point.rnw(),
                point.mw(),
                point.hsw(),
                point.hw100());
    }

    private static MonitoringResponse.StationSummary toSummary(Station station) {
        return new MonitoringResponse.StationSummary(
                station.id().value(), station.stationNumber(), station.name(), station.waterBody());
    }

    private static MonitoringResponse.StationOwner toOwner(StationOwner owner) {
        return new MonitoringResponse.StationOwner(owner.id().value(), owner.name(), owner.shortName());
    }

    private static MonitoringResponse.LatestMeasurement toLatest(LatestMeasurement measurement) {
        return measurement == null
                ? null
                : new MonitoringResponse.LatestMeasurement(measurement.observedAt(), measurement.value());
    }
}
