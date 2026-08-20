package at.pegelhub.stationowner.application;

public record UpdateStationOwnerCommand(
        String name,
        String shortName,
        String notes
) {
}
