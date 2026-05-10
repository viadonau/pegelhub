package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Connector;
import at.pegelhub.lib.model.Contact;

public record ConnectorReceiveDto(String id, Contact manufacturer, String typeDescription, double softwareVersion, double worksFromDataVersion, String dataDefinition, Contact softwareManufacturer, Contact technicallyResponsible, Contact operatingCompany, String nodes) {
    public Connector toConnector() {
        var con = new Connector();
        con.setManufacturerId(manufacturer.getId());
        con.setTypeDescription(typeDescription);
        con.setSoftwareVersion(softwareVersion);
        con.setWorksFromDataVersion(worksFromDataVersion);
        con.setDataDefinition(dataDefinition);
        con.setSoftwareManufacturerId(softwareManufacturer.getId());
        con.setTechnicallyResponsibleId(technicallyResponsible.getId());
        con.setOperatingCompanyId(operatingCompany.getId());
        con.setNodes(nodes);
        return con;
    }
}
