package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.application.CreateMeasuringPointCommand;
import at.pegelhub.measuringpoint.domain.BankSide;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.station.domain.StationId;

final class MeasuringPointMapper {

    private MeasuringPointMapper() {
    }

    static CreateMeasuringPointCommand toCommand(CreateMeasuringPointRequest request) {
        return new CreateMeasuringPointCommand(
                new StationId(request.stationId()),
                request.name(),
                request.referenceLevel(),
                request.referenceYear(),
                request.riverKilometer(),
                BankSide.fromNullable(request.bank()),
                request.rnw(),
                request.mw(),
                request.hsw(),
                request.hw100());
    }

    static MeasuringPointResponse toResponse(MeasuringPoint measuringPoint) {
        return new MeasuringPointResponse(
                measuringPoint.id().value(),
                measuringPoint.stationId().value(),
                measuringPoint.name(),
                measuringPoint.referenceLevel(),
                measuringPoint.referenceYear(),
                measuringPoint.riverKilometer(),
                measuringPoint.bank() == null ? null : measuringPoint.bank().value(),
                measuringPoint.rnw(),
                measuringPoint.mw(),
                measuringPoint.hsw(),
                measuringPoint.hw100());
    }
}
