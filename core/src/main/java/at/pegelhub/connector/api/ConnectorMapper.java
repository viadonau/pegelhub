package at.pegelhub.connector.api;

import at.pegelhub.connector.application.CreateConnectorCommand;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.contact.api.ContactMapper;

final class ConnectorMapper {

    private ConnectorMapper() {
    }

    static ConnectorDto toResponse(Connector connector) {
        return new ConnectorDto(
                connector.id().value(),
                connector.connectorNumber(),
                ContactMapper.toResponse(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                ContactMapper.toResponse(connector.softwareManufacturer()),
                ContactMapper.toResponse(connector.technicallyResponsible()),
                ContactMapper.toResponse(connector.operationCompany()),
                connector.notes(),
                connector.keycloakClientId(),
                ConnectorStatusDto.fromDomain(connector.status()));
    }

    static CreateConnectorCommand toCommand(CreateConnectorDto dto) {
        return new CreateConnectorCommand(
                dto.connectorNumber(),
                dto.manufacturer(),
                dto.typeDescription(),
                dto.softwareVersion(),
                dto.worksFromDataVersion(),
                dto.dataDefinition(),
                dto.softwareManufacturer(),
                dto.technicallyResponsible(),
                dto.operationCompany(),
                dto.notes());
    }
}
