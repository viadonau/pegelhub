package at.pegelhub.testsupport;

import at.pegelhub.connector.api.ConnectorDto;
import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.contact.api.ContactDto;
import at.pegelhub.contact.api.CreateContactDto;

import java.util.UUID;

public class ExampleDtos {

    private static final UUID uuid = UUID.randomUUID();
    public static final String ORGANIZATION = "org1";
    public static final String CONTACT_PERSON = "Hans Maier";
    public static final String CONTACT_STREET = "Blumenweg 22";
    public static final String CONTACT_PLZ = "1549";
    public static final String LOCATION = "Wien";
    public static final String CONTACT_COUNTRY = "AT";
    public static final String EMERGENCY_NUMBER = "123456789";
    public static final String EMERGENCY_NUMBER_TWO = "123456780";
    public static final String EMERGENCY_MAIL = "emergency@mail.com";
    public static final String SERVICE_NUMBER = "123456789";
    public static final String SERVICE_NUMBER_TWO = "123456780";
    public static final String SERVICE_MAIL = "service@mail.com";
    public static final String ADMIN_NUMBER = "123456789";
    public static final String ADMIN_NUMBER_TWO = "123456780";
    public static final String ADMIN_MAIL = "service@mail.com";
    public static final String NOTES = "notes";
    public static final String CONNECTOR_NUMBER = "connectorNR";
    public static final String DESCRIPTION = "description";
    public static final String VERSION = "1.0.0";
    public static final String DATA_DEFINITION = "definition";

    public static final CreateContactDto CREATE_CONTACT_DTO = new CreateContactDto(ORGANIZATION, CONTACT_PERSON,
            CONTACT_STREET, CONTACT_PLZ, LOCATION, CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO,
            EMERGENCY_MAIL, SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO,
            ADMIN_MAIL, NOTES);

    public static final CreateConnectorDto CREATE_CONNECTOR_DTO = new CreateConnectorDto(CONNECTOR_NUMBER, CREATE_CONTACT_DTO, DESCRIPTION, VERSION,
            VERSION, DATA_DEFINITION, CREATE_CONTACT_DTO, CREATE_CONTACT_DTO, CREATE_CONTACT_DTO, NOTES);

    public static final ContactDto CONTACT_DTO = new ContactDto(uuid, ORGANIZATION, CONTACT_PERSON,
            CONTACT_STREET, CONTACT_PLZ, LOCATION, CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO,
            EMERGENCY_MAIL, SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO,
            ADMIN_MAIL, NOTES);

    public static final ConnectorDto CONNECTOR_DTO = new ConnectorDto(uuid, CONNECTOR_NUMBER, CONTACT_DTO, DESCRIPTION,
            VERSION, VERSION, DATA_DEFINITION, CONTACT_DTO, CONTACT_DTO, CONTACT_DTO, NOTES);
}
