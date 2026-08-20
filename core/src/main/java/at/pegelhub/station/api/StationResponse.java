package at.pegelhub.station.api;

import at.pegelhub.shared.metadata.MetadataStatus;

import java.util.UUID;

public record StationResponse(UUID id, UUID ownerId, String name, String waterBody, MetadataStatus status) { }
