package at.pegelhub.monitoring.api;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.monitoring.application.MonitoringCollection;
import at.pegelhub.monitoring.application.MonitoringDetail;
import at.pegelhub.station.domain.Station;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.timeseries.domain.TimeSeries;

public final class MonitoringResponseMapper {
    private MonitoringResponseMapper() { }

    public static MonitoringResponse.TimeSeriesCollection toResponse(MonitoringCollection collection) {
        return new MonitoringResponse.TimeSeriesCollection(
                collection.items().stream().map(item -> new MonitoringResponse.TimeSeriesSummary(
                        item.timeSeries().id().value(), item.timeSeries().observedProperty().value(), item.timeSeries().unit(),
                        new MonitoringResponse.MeasuringPointSummary(item.measuringPoint().id().value(), item.measuringPoint().name()),
                        toSummary(item.station()), toLatest(item.latestMeasurement()))).toList());
    }

    public static MonitoringResponse.TimeSeriesDetail toResponse(MonitoringDetail detail) {
        TimeSeries series = detail.timeSeries();
        return new MonitoringResponse.TimeSeriesDetail(
                series.id().value(), series.observedProperty().value(), series.unit(), detail.status(),
                toMeasuringPoint(detail.measuringPoint()), toSummary(detail.station()), toOwner(detail.stationOwner()),
                toLatest(detail.latestMeasurement()));
    }

    private static MonitoringResponse.MeasuringPoint toMeasuringPoint(MeasuringPoint point) {
        var position = point.position();
        var refs = point.waterLevelReferences();
        return new MonitoringResponse.MeasuringPoint(
                point.id().value(), point.name(), point.status(),
                position == null ? null : new MonitoringResponse.Position(
                        position.riverKilometer(), position.bank() == null ? null : position.bank().value()),
                point.gaugeZeroElevationMAboveAdria(),
                refs == null ? null : new MonitoringResponse.WaterLevelReferences(
                        refs.referenceSetYear(), refs.rnwCm(), refs.mwCm(), refs.hswCm(), refs.hw100Cm()));
    }

    private static MonitoringResponse.StationSummary toSummary(Station station) {
        return new MonitoringResponse.StationSummary(station.id().value(), station.name(), station.waterBody());
    }

    private static MonitoringResponse.StationOwner toOwner(StationOwner owner) {
        return new MonitoringResponse.StationOwner(owner.id().value(), owner.name(), owner.shortName());
    }

    private static MonitoringResponse.LatestMeasurement toLatest(LatestMeasurement measurement) {
        return measurement == null ? null : new MonitoringResponse.LatestMeasurement(measurement.observedAt(), measurement.value());
    }
}
