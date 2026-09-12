package at.pegelhub.notifications.domain;

import java.util.UUID;

public record Destination(UUID id, DestinationConfig configuration) { }
