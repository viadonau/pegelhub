package at.pegelhub.station.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Station metadata.")
public record StationResponse(
        @Schema(description = "Station identifier.", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        UUID id,
        @Schema(description = "Station owner identifier.", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
        UUID ownerId,
        @Schema(description = "External station number.", example = "207241")
        String stationNumber,
        @Schema(description = "Human-readable station name.", example = "Wien Reichsbruecke")
        String name,
        @Schema(description = "Water body observed by the station.", example = "Donau")
        String waterBody,
        @Schema(description = "Optional free-text station location.", nullable = true, example = "Left bank near bridge pillar")
        String location
) {
}
