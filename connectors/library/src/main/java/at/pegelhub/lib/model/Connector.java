package at.pegelhub.lib.model;

/**
 * The model class used to send and receive {@code Connector} objects.
 */
public class Connector {
    private String manufacturerId;
    private String typeDescription;
    private double softwareVersion;
    private double worksFromDataVersion;
    private String dataDefinition;
    private String softwareManufacturerId;
    private String technicallyResponsibleId;
    private String operatingCompanyId;
    private String nodes;

    public Connector() {
        this.manufacturerId = "";
        this.typeDescription = "";
        this.dataDefinition = "";
        this.softwareManufacturerId = "";
        this.technicallyResponsibleId = "";
        this.operatingCompanyId = "";
        this.nodes = "";
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(String manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public double getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(double softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public double getWorksFromDataVersion() {
        return worksFromDataVersion;
    }

    public void setWorksFromDataVersion(double worksFromDataVersion) {
        this.worksFromDataVersion = worksFromDataVersion;
    }

    public String getDataDefinition() {
        return dataDefinition;
    }

    public void setDataDefinition(String dataDefinition) {
        this.dataDefinition = dataDefinition;
    }

    public String getSoftwareManufacturerId() {
        return softwareManufacturerId;
    }

    public void setSoftwareManufacturerId(String softwareManufacturerId) {
        this.softwareManufacturerId = softwareManufacturerId;
    }

    public String getTechnicallyResponsibleId() {
        return technicallyResponsibleId;
    }

    public void setTechnicallyResponsibleId(String technicallyResponsibleId) {
        this.technicallyResponsibleId = technicallyResponsibleId;
    }

    public String getOperatingCompanyId() {
        return operatingCompanyId;
    }

    public void setOperatingCompanyId(String operatingCompanyId) {
        this.operatingCompanyId = operatingCompanyId;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }
}
