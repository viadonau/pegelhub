package at.pegelhub.lib.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import at.pegelhub.lib.internal.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton-class for application properties provided by user of library.
 */
public class ApplicationPropertiesImpl implements ApplicationProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPropertiesImpl.class);
    private final boolean isSupplier;
    private SupplierSendDto supplier;
    private TakerSendDto taker;

    private final Map<String, Object> data;
    private final ObjectMapper mapper;
    private final File file;
    private boolean supplierDataIsToSend;

    private ObjectMapper objectMapper;

    public ApplicationPropertiesImpl(String propertiesFile) {
        this.mapper = new YAMLMapper();
        // enabling datetime serialization module
        this.mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.file = new File(propertiesFile);
        try {
            this.data = mapper.readValue(file, new TypeReference<>() { });
            this.isSupplier = (boolean)data.get("isSupplier");
            readMetaData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.supplierDataIsToSend = metadataShouldBeSentOnStartup();
    }

    @Override
    public boolean isSupplier() {
        return isSupplier;
    }

    @Override
    public SupplierSendDto getSupplier() {
        return supplier;
    }

    @Override
    public TakerSendDto getTaker() {
        return taker;
    }

    @Override
    public boolean isSupplierDataToSend() {
        if (supplierDataIsToSend) {
            supplierDataIsToSend = false;
            return true;
        }
        return false;
    }

    @Override
    public void setUtcIsUsed(Boolean utcIsUsed) {
        HashMap<String, Object> supplierproperties = (HashMap<String, Object>) data.get("supplier");
        supplierproperties.put("utcIsUsed", utcIsUsed);
        put("supplier", supplierproperties);
    }

    @Override
    public boolean isRefreshNecessary() {
        return false;
    }

    @Override
    public String getTokenUrl() {
        return keycloakValue("tokenUrl");
    }

    @Override
    public String getClientId() {
        return keycloakValue("clientId");
    }

    @Override
    public String getClientSecret() {
        return keycloakValue("clientSecret");
    }

    private boolean metadataShouldBeSentOnStartup() {
        return metadataShouldBeSentOnStartup(data);
    }

    static boolean metadataShouldBeSentOnStartup(Map<String, Object> data) {
        Object value = data.get("sendMetaDataOnStartup");
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private void readMetaData() {
        if (this.isSupplier()) {
            readSupplierData();
        }
        else {
            readTakerData();
        }
    }

    private void readSupplierData() {
        Map<String, Object> supplierData = (Map<String, Object>)data.get("supplier");
        Map<String, Object> manufacturerData = (Map<String, Object>)supplierData.get("manufacturer");
        Map<String, Object> connectorData = (Map<String, Object>)supplierData.get("connector");
        Map<String, Object> connector_contactManufacturerData = (Map<String, Object>)connectorData.get("contact_manufacturer");
        Map<String, Object> connector_contactSoftwareManufacturerData = (Map<String, Object>)connectorData.get("contact_softwareManufacturer");
        Map<String, Object> connector_contactTechnicallyResponsibleData = (Map<String, Object>)connectorData.get("contact_technicallyResponsible");
        Map<String, Object> connector_contactOperationCompanyData = (Map<String, Object>)connectorData.get("contact_operationCompany");
        supplier = new SupplierSendDto(
                (String)supplierData.get("number"),
                (Integer) supplierData.get("id"),
                (String)supplierData.get("name"),
                (String)supplierData.get("water"),
                ((String)supplierData.get("waterType")).charAt(0),
                new StationManufacturerSendDto(
                        (String)manufacturerData.get("name"),
                        (String)manufacturerData.get("type"),
                        (String)manufacturerData.get("version"),
                        (String)manufacturerData.get("remark")
                ),
                new CompleteConnectorSendDto(
                        (String)connectorData.get("number"),
                        new ContactSendDto(
                                (String)connector_contactManufacturerData.get("organization"),
                                (String)connector_contactManufacturerData.get("contactPerson"),
                                (String)connector_contactManufacturerData.get("contactStreet"),
                                (String)connector_contactManufacturerData.get("contactPlz"),
                                (String)connector_contactManufacturerData.get("location"),
                                (String)connector_contactManufacturerData.get("contactCountry"),
                                (String)connector_contactManufacturerData.get("emergencyNumber"),
                                (String)connector_contactManufacturerData.get("emergencyNumberTwo"),
                                (String)connector_contactManufacturerData.get("emergencyMail"),
                                (String)connector_contactManufacturerData.get("serviceNumber"),
                                (String)connector_contactManufacturerData.get("serviceNumberTwo"),
                                (String)connector_contactManufacturerData.get("serviceMail"),
                                (String)connector_contactManufacturerData.get("administrationPhoneNumber"),
                                (String)connector_contactManufacturerData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactManufacturerData.get("administrationMail"),
                                (String)connector_contactManufacturerData.get("contactNodes")
                        ),
                        (String)connectorData.get("typeDescription"),
                        (String)connectorData.get("softwareVersion"),
                        (String)connectorData.get("worksFromDataVersion"),
                        (String)connectorData.get("dataDefinition"),
                        new ContactSendDto(
                                (String)connector_contactSoftwareManufacturerData.get("organization"),
                                (String)connector_contactSoftwareManufacturerData.get("contactPerson"),
                                (String)connector_contactSoftwareManufacturerData.get("contactStreet"),
                                (String)connector_contactSoftwareManufacturerData.get("contactPlz"),
                                (String)connector_contactSoftwareManufacturerData.get("location"),
                                (String)connector_contactSoftwareManufacturerData.get("contactCountry"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyMail"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceMail"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationPhoneNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationMail"),
                                (String)connector_contactSoftwareManufacturerData.get("contactNodes")
                        ),
                        new ContactSendDto(
                                (String)connector_contactTechnicallyResponsibleData.get("organization"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactPerson"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactStreet"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactPlz"),
                                (String)connector_contactTechnicallyResponsibleData.get("location"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactCountry"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationPhoneNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactNodes")
                        ),
                        new ContactSendDto(
                                (String)connector_contactOperationCompanyData.get("organization"),
                                (String)connector_contactOperationCompanyData.get("contactPerson"),
                                (String)connector_contactOperationCompanyData.get("contactStreet"),
                                (String)connector_contactOperationCompanyData.get("contactPlz"),
                                (String)connector_contactOperationCompanyData.get("location"),
                                (String)connector_contactOperationCompanyData.get("contactCountry"),
                                (String)connector_contactOperationCompanyData.get("emergencyNumber"),
                                (String)connector_contactOperationCompanyData.get("emergencyNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("emergencyMail"),
                                (String)connector_contactOperationCompanyData.get("serviceNumber"),
                                (String)connector_contactOperationCompanyData.get("serviceNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("serviceMail"),
                                (String)connector_contactOperationCompanyData.get("administrationPhoneNumber"),
                                (String)connector_contactOperationCompanyData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("administrationMail"),
                                (String)connector_contactOperationCompanyData.get("contactNodes")
                        ),
                        (String)connectorData.get("notes")
                ),
                (Integer) supplierData.get("refreshRate"),
                (Double) supplierData.get("accuracy"),
                (String) supplierData.get("mainUsage"),
                (String)supplierData.get("dataCritically"),
                (Double) supplierData.get("baseReferenceLevel"),
                (String) supplierData.get("referencePlace"),
                (Double) supplierData.get("waterKilometer"),
                (String) supplierData.get("waterSide"),
                (Double) supplierData.get("waterLatitude"),
                (Double) supplierData.get("waterLongitude"),
                (Double) supplierData.get("waterLatitudem"),
                (Double) supplierData.get("waterLongitudem"),
                (Double) supplierData.get("hsw100"),
                (Double) supplierData.get("hsw"),
                (Integer) supplierData.get("hswReference"),
                (Double) supplierData.get("mw"),
                (Integer) supplierData.get("mwReference"),
                (Double) supplierData.get("rnw"),
                (Integer) supplierData.get("rnwReference"),
                (Double) supplierData.get("hsq100"),
                (Double) supplierData.get("hsq"),
                (Double) supplierData.get("mq"),
                (Double) supplierData.get("rnq"),
                (String) supplierData.get("channelUse"),
                (Boolean)supplierData.get("utcIsUsed"),
                (Boolean)supplierData.get("isSummertime")
        );
    }

    private void readTakerData() {
        Map<String, Object> takerData = (Map<String, Object>)data.get("taker");
        Map<String, Object> manufacturerData = (Map<String, Object>)takerData.get("manufacturer");
        Map<String, Object> connectorData = (Map<String, Object>)takerData.get("connector");
        Map<String, Object> connector_contactManufacturerData = (Map<String, Object>)connectorData.get("contact_manufacturer");
        Map<String, Object> connector_contactSoftwareManufacturerData = (Map<String, Object>)connectorData.get("contact_softwareManufacturer");
        Map<String, Object> connector_contactTechnicallyResponsibleData = (Map<String, Object>)connectorData.get("contact_technicallyResponsible");
        Map<String, Object> connector_contactOperationCompanyData = (Map<String, Object>)connectorData.get("contact_operationCompany");
        taker = new TakerSendDto(
                (String)takerData.get("number"),
                (Integer) takerData.get("id"),
                new TakerServiceManufacturerSendDto(
                        (String)manufacturerData.get("name"),
                        (String)manufacturerData.get("systemName"),
                        (String)manufacturerData.get("firmwareVersion"),
                        (String)manufacturerData.get("remark")
                ),
                new CompleteConnectorSendDto(
                        (String)connectorData.get("number"),
                        new ContactSendDto(
                                (String)connector_contactManufacturerData.get("organization"),
                                (String)connector_contactManufacturerData.get("contactPerson"),
                                (String)connector_contactManufacturerData.get("contactStreet"),
                                (String)connector_contactManufacturerData.get("contactPlz"),
                                (String)connector_contactManufacturerData.get("location"),
                                (String)connector_contactManufacturerData.get("contactCountry"),
                                (String)connector_contactManufacturerData.get("emergencyNumber"),
                                (String)connector_contactManufacturerData.get("emergencyNumberTwo"),
                                (String)connector_contactManufacturerData.get("emergencyMail"),
                                (String)connector_contactManufacturerData.get("serviceNumber"),
                                (String)connector_contactManufacturerData.get("serviceNumberTwo"),
                                (String)connector_contactManufacturerData.get("serviceMail"),
                                (String)connector_contactManufacturerData.get("administrationPhoneNumber"),
                                (String)connector_contactManufacturerData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactManufacturerData.get("administrationMail"),
                                (String)connector_contactManufacturerData.get("contactNodes")
                        ),
                        (String)connectorData.get("typeDescription"),
                        (String)connectorData.get("softwareVersion"),
                        (String)connectorData.get("worksFromDataVersion"),
                        (String)connectorData.get("dataDefinition"),
                        new ContactSendDto(
                                (String)connector_contactSoftwareManufacturerData.get("organization"),
                                (String)connector_contactSoftwareManufacturerData.get("contactPerson"),
                                (String)connector_contactSoftwareManufacturerData.get("contactStreet"),
                                (String)connector_contactSoftwareManufacturerData.get("contactPlz"),
                                (String)connector_contactSoftwareManufacturerData.get("location"),
                                (String)connector_contactSoftwareManufacturerData.get("contactCountry"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("emergencyMail"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("serviceMail"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationPhoneNumber"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactSoftwareManufacturerData.get("administrationMail"),
                                (String)connector_contactSoftwareManufacturerData.get("contactNodes")
                        ),
                        new ContactSendDto(
                                (String)connector_contactTechnicallyResponsibleData.get("organization"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactPerson"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactStreet"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactPlz"),
                                (String)connector_contactTechnicallyResponsibleData.get("location"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactCountry"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("emergencyMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("serviceMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationPhoneNumber"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactTechnicallyResponsibleData.get("administrationMail"),
                                (String)connector_contactTechnicallyResponsibleData.get("contactNodes")
                        ),
                        new ContactSendDto(
                                (String)connector_contactOperationCompanyData.get("organization"),
                                (String)connector_contactOperationCompanyData.get("contactPerson"),
                                (String)connector_contactOperationCompanyData.get("contactStreet"),
                                (String)connector_contactOperationCompanyData.get("contactPlz"),
                                (String)connector_contactOperationCompanyData.get("location"),
                                (String)connector_contactOperationCompanyData.get("contactCountry"),
                                (String)connector_contactOperationCompanyData.get("emergencyNumber"),
                                (String)connector_contactOperationCompanyData.get("emergencyNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("emergencyMail"),
                                (String)connector_contactOperationCompanyData.get("serviceNumber"),
                                (String)connector_contactOperationCompanyData.get("serviceNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("serviceMail"),
                                (String)connector_contactOperationCompanyData.get("administrationPhoneNumber"),
                                (String)connector_contactOperationCompanyData.get("administrationPhoneNumberTwo"),
                                (String)connector_contactOperationCompanyData.get("administrationMail"),
                                (String)connector_contactOperationCompanyData.get("contactNodes")
                        ),
                        (String)connectorData.get("notes")
                ),
                (Integer) takerData.get("refreshRate")
        );
    }


    private String keycloakValue(String key) {
        Map<String, Object> keycloak = (Map<String, Object>) data.get("keycloak");
        return keycloak == null ? null : (String) keycloak.get(key);
    }


    private <T> T get(String key) {
        return (T)data.get(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
        syncProperties();
    }

    private void syncProperties() {
        try {
            this.mapper.writeValue(this.file, this.data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        readMetaData();
    }

    public Map<String, Object> getProperties()
    {
        return this.data;
    }
}
