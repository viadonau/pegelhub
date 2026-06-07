package at.pegelhub.timeseries.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

        @Positive
        Long expectedIntervalSeconds,

        @Size(max = 160)
        String externalCode
) {
}
