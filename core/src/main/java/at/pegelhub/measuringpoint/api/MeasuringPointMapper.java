package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.application.CreateMeasuringPointCommand;
import at.pegelhub.measuringpoint.application.UpdateMeasuringPointCommand;
import at.pegelhub.measuringpoint.domain.Coordinates;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.measuringpoint.domain.WaterLevelReferences;
import at.pegelhub.station.domain.StationId;

final class MeasuringPointMapper {
    private MeasuringPointMapper() { }

    static CreateMeasuringPointCommand toCommand(CreateMeasuringPointRequest request) {
        return new CreateMeasuringPointCommand(
                new StationId(request.stationId()), request.name(), request.status(), position(request.position()),
                request.gaugeZeroElevationMAboveAdria(), references(request.waterLevelReferences()));
    }

    static UpdateMeasuringPointCommand toCommand(UpdateMeasuringPointRequest request) {
        return new UpdateMeasuringPointCommand(
                request.name(), request.status(), position(request.position()),
                request.gaugeZeroElevationMAboveAdria(), references(request.waterLevelReferences()));
    }

    static MeasuringPointResponse toResponse(MeasuringPoint point) {
        var position = point.position();
        var coordinates = position == null ? null : position.coordinates();
        var refs = point.waterLevelReferences();
        return new MeasuringPointResponse(
                point.id().value(), point.stationId().value(), point.name(), point.status(),
                position == null ? null : new MeasuringPointResponse.PositionResponse(
                        position.riverKilometer(), position.bank() == null ? null : position.bank().value(),
                        coordinates == null ? null : new MeasuringPointResponse.CoordinatesResponse(coordinates.latitude(), coordinates.longitude())),
                point.gaugeZeroElevationMAboveAdria(),
                refs == null ? null : new MeasuringPointResponse.WaterLevelReferencesResponse(
                        refs.referenceSetYear(), refs.rnwCm(), refs.mwCm(), refs.hswCm(), refs.hw100Cm()));
    }

    private static MeasuringPointPosition position(PositionRequest request) {
        if (request == null) return null;
        Coordinates coordinates = request.coordinates() == null ? null
                : new Coordinates(request.coordinates().latitude(), request.coordinates().longitude());
        return new MeasuringPointPosition(request.riverKilometer(), request.bank(), coordinates);
    }

    private static WaterLevelReferences references(WaterLevelReferencesRequest request) {
        return request == null ? null : new WaterLevelReferences(
                request.referenceSetYear(), request.rnwCm(), request.mwCm(), request.hswCm(), request.hw100Cm());
    }
}
