package at.pegelhub.stationowner.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Station owner metadata.")
public record StationOwnerResponse(
        @Schema(description = "Station owner identifier.", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
        UUID id,
        @Schema(description = "Legal or organizational station owner name.", example = "Hydrographic Service Vienna")
        String name,
        @Schema(description = "Optional short name.", nullable = true, example = "HS Vienna")
        String shortName,
        @Schema(description = "Optional operational notes.", nullable = true, example = "Operates Danube stations in Vienna.")
        String notes
) {
}
