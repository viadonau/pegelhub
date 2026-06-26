package at.pegelhub.timeseries.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTimeSeriesRequest(
        @NotNull
        UUID stationId,

        @NotBlank
        @Size(max = 120)
        String observedProperty,

        @NotBlank
        @Size(max = 40)
        String unit,

        Double referenceLevel,

        @Min(1)
        @Max(9999)
        Integer referenceYear,

        Double riverKilometer,

        @Size(max = 40)
        String bank,

        Double rnw,

        Double hsw,

        Double mw,

        Double hw100,

        @Size(max = 160)
        String externalCode,

        UUID sourceConnectorId
) {
}
