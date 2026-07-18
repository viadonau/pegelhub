package at.pegelhub.connector.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Connector lifecycle status.", enumAsRef = true)
public enum ConnectorStatus {
    ACTIVE,
    SUSPENDED
}
