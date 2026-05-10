package at.pegelhub.lib.internal.dto;

import java.util.UUID;

public record ConnectorSendDto(UUID id, String manufacturerId,
                               String typeDescription, String softwareVersion, String worksFromDataVersion, String dataDefinition,
                               String softwareManufacturerId, String technicallyResponsibleId, String operatingCompanyId,
                               String nodes) {}
