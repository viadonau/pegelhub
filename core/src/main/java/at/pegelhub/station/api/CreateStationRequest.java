package at.pegelhub.station.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "openapi.station.create-station-request.request-to-create-station-metadata")
public record CreateStationRequest(
        @Schema(description = "openapi.station.create-station-request.station-owner-identifier", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
        @NotNull
        UUID ownerId,

        @Schema(description = "openapi.station.create-station-request.external-station-number", maxLength = 80, example = "207241")
        @NotBlank
        @Size(max = 80)
        String stationNumber,

        @Schema(description = "openapi.station.create-station-request.human-readable-station-name", maxLength = 200, example = "Wien Reichsbruecke")
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "openapi.station.create-station-request.water-body-observed-by-the-station", maxLength = 200, example = "Donau")
        @NotBlank
        @Size(max = 200)
        String waterBody,

        @Schema(description = "openapi.station.create-station-request.optional-free-text-station-location", maxLength = 500, example = "Left bank near bridge pillar", nullable = true)
        @Size(max = 500)
        String location
) {
}
