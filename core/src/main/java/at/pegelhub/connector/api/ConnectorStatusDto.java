package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConnectorStatus", description = "Connector lifecycle status.", enumAsRef = true)
public enum ConnectorStatusDto {
    ACTIVE,
    SUSPENDED;

    ConnectorStatus toDomain() {
        return ConnectorStatus.valueOf(name());
    }

    static ConnectorStatusDto fromDomain(ConnectorStatus status) {
        return ConnectorStatusDto.valueOf(status.name());
    }
}
