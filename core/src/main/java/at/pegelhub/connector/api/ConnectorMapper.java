package at.pegelhub.connector.api;

import at.pegelhub.connector.application.CreateConnectorCommand;
import at.pegelhub.connector.application.UpdateConnectorCommand;
import at.pegelhub.connector.domain.Connector;

final class ConnectorMapper {
    private ConnectorMapper() {
    }

    static ConnectorDto toResponse(Connector connector) {
        return new ConnectorDto(
                connector.id().value(), connector.name(), connector.type(), connector.keycloakClientId(), connector.status());
    }

    static CreateConnectorCommand toCommand(CreateConnectorDto dto) {
        return new CreateConnectorCommand(dto.name(), dto.type());
    }

    static UpdateConnectorCommand toCommand(UpdateConnectorRequest request) {
        return new UpdateConnectorCommand(request.name(), request.type(), request.status());
    }
}
