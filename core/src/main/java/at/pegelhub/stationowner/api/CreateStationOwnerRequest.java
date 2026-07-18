package at.pegelhub.stationowner.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create station owner metadata.")
public record CreateStationOwnerRequest(
        @Schema(description = "Legal or organizational station owner name.", maxLength = 200, example = "Hydrographic Service Vienna")
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Optional short name.", maxLength = 80, example = "HS Vienna", nullable = true)
        @Size(max = 80)
        String shortName,

        @Schema(description = "Optional operational notes.", maxLength = 2000, example = "Operates Danube stations in Vienna.", nullable = true)
        @Size(max = 2_000)
        String notes
) {
}
