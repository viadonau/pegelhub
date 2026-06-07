package at.pegelhub.stationowner.application;

public record CreateStationOwnerCommand(
        String name,
        String shortName,
        String notes
) {
}
