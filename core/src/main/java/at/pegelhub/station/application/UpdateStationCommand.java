package at.pegelhub.station.application;

import at.pegelhub.shared.metadata.MetadataStatus;

public record UpdateStationCommand(String name, String waterBody, MetadataStatus status) { }
