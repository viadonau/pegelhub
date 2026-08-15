package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.BankSide;
import at.pegelhub.station.domain.StationId;

public record CreateMeasuringPointCommand(
        StationId stationId,
        String name,
        Double referenceLevel,
        Integer referenceYear,
        Double riverKilometer,
        BankSide bank,
        Double rnw,
        Double mw,
        Double hsw,
        Double hw100
) {
}
