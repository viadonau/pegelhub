package at.pegelhub.measuringpoint.domain;

import at.pegelhub.station.domain.StationId;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeRequired;
import static java.util.Objects.requireNonNull;

public record MeasuringPoint(
        MeasuringPointId id,
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

    public MeasuringPoint {
        requireNonNull(id);
        requireNonNull(stationId);
        name = normalizeRequired(name, "Measuring point name must not be blank");
        referenceLevel = requireFiniteOptional(referenceLevel, "referenceLevel");
        referenceYear = requireReferenceYear(referenceYear);
        riverKilometer = requireFiniteOptional(riverKilometer, "riverKilometer");
        rnw = requireFiniteOptional(rnw, "rnw");
        mw = requireFiniteOptional(mw, "mw");
        hsw = requireFiniteOptional(hsw, "hsw");
        hw100 = requireFiniteOptional(hw100, "hw100");
    }

    public static MeasuringPoint create(
            StationId stationId,
            String name,
            Double referenceLevel,
            Integer referenceYear,
            Double riverKilometer,
            BankSide bank,
            Double rnw,
            Double mw,
            Double hsw,
            Double hw100) {
        return new MeasuringPoint(
                new MeasuringPointId(UUID.randomUUID()),
                stationId,
                name,
                referenceLevel,
                referenceYear,
                riverKilometer,
                bank,
                rnw,
                mw,
                hsw,
                hw100);
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
