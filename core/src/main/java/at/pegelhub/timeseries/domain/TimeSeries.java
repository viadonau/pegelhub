package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeOptional;
import static java.util.Objects.requireNonNull;

public record TimeSeries(
        TimeSeriesId id,
        StationId stationId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        Double referenceLevel,
        Integer referenceYear,
        /*
         * V1 keeps PNP, gauge-location, and water-level reference metadata on TimeSeries.
         * Extract a MeasuringPoint once multiple series share a bank/km/reference set or
         * left/right/device identity needs an independent lifecycle.
         */
        Double riverKilometer,
        String bank,
        Double rnw,
        Double hsw,
        Double mw,
        Double hw100,
        ExternalTimeSeriesCode externalCode,
        ConnectorId sourceConnectorId
) {

    public TimeSeries {
        requireNonNull(id);
        requireNonNull(stationId);
        requireNonNull(observedProperty);
        requireNonNull(unit);
        referenceLevel = requireFiniteOptional(referenceLevel, "referenceLevel");
        referenceYear = requireReferenceYear(referenceYear);
        riverKilometer = requireFiniteOptional(riverKilometer, "riverKilometer");
        bank = normalizeOptional(bank);
        rnw = requireFiniteOptional(rnw, "rnw");
        hsw = requireFiniteOptional(hsw, "hsw");
        mw = requireFiniteOptional(mw, "mw");
        hw100 = requireFiniteOptional(hw100, "hw100");
    }

    public static TimeSeries create(
            StationId stationId,
            ObservedPropertyCode observedProperty,
            UnitCode unit,
            Double referenceLevel,
            Integer referenceYear,
            Double riverKilometer,
            String bank,
            Double rnw,
            Double hsw,
            Double mw,
            Double hw100,
            ExternalTimeSeriesCode externalCode,
            ConnectorId sourceConnectorId) {
        return new TimeSeries(
                new TimeSeriesId(UUID.randomUUID()),
                stationId,
                observedProperty,
                unit,
                referenceLevel,
                referenceYear,
                riverKilometer,
                bank,
                rnw,
                hsw,
                mw,
                hw100,
                externalCode,
                sourceConnectorId);
    }

    private static Double requireFiniteOptional(Double value, String fieldName) {
        if (value != null && !Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return value;
    }

    private static Integer requireReferenceYear(Integer value) {
        if (value != null && (value < 1 || value > 9999)) {
            throw new IllegalArgumentException("referenceYear must be a valid calendar year");
        }
        return value;
    }
}
