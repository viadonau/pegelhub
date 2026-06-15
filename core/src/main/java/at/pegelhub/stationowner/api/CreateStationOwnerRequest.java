package at.pegelhub.stationowner.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStationOwnerRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 80)
        String shortName,

        @Size(max = 2_000)
        String notes
) {
}
