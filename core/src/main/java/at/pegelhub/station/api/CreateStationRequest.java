package at.pegelhub.station.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStationRequest(
        @NotNull
        UUID ownerId,

        @NotBlank
        @Size(max = 80)
        String stationNumber,

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 200)
        String waterBody,

        @Size(max = 500)
        String location
) {
}
