package at.pegelhub.testsupport;

import at.pegelhub.connector.api.ConnectorDto;
import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.contact.api.ContactDto;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.supplier.api.CreateStationManufacturerDto;
import at.pegelhub.supplier.api.CreateSupplierDto;
import at.pegelhub.supplier.api.StationManufacturerDto;
import at.pegelhub.taker.api.CreateTakerDto;
import at.pegelhub.taker.api.CreateTakerServiceManufacturerDto;
import at.pegelhub.taker.api.TakerServiceManufacturerDto;

import java.util.UUID;

/**
 * Class for test example data of dtos
 */
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
    public static final String MANUFACTURER_NAME = "name";
    public static final String SYSTEM_NAME = "name";
    public static final String MANUFACTURER_TYPE = "type";
    public static final String REMARK = "remarks";
    public static final String STATION_NUMBER = "stationNR";
    public static final Integer STATION_ID = 4143365;
    public static final String STATION_NAME = "stationName";
    public static final String STATION_WATER = "stationWater";
    public static final char STATION_WATER_TYPE = 't';
    public static final double ACCURACY = 1.0;
    public static final String MAIN_USAGE = "do smth";
    public static final String DATA_CRITICALLY = "true";
    public static final double REFERENCE_LEVEL = 0.5;
    public static final String REFERENCE_PLACE = "place";
    public static final double WATER_KILOMETER = 50;
    public static final String WATER_SIDE = "side";
    public static final double WATER_LAT = 45.5;
    public static final double WATER_LONG = 45.5;
    public static final double HSW = 45.5;
    public static final int HSW_REF = 45;
    public static final long REFRESH_RATE = 100;
    public static final Boolean UTC_IS_USED = false;
    public static final Boolean IS_SUMMERTIME = false;
    public static final String CHANNEL_USE = "TestUse";
    //region create dto
    public static final CreateContactDto CREATE_CONTACT_DTO = new CreateContactDto(ORGANIZATION, CONTACT_PERSON,
            CONTACT_STREET, CONTACT_PLZ, LOCATION, CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO,
            EMERGENCY_MAIL, SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO,
            ADMIN_MAIL, NOTES);

    public static final CreateConnectorDto CREATE_CONNECTOR_DTO = new CreateConnectorDto(CONNECTOR_NUMBER, CREATE_CONTACT_DTO, DESCRIPTION, VERSION,
            VERSION, DATA_DEFINITION, CREATE_CONTACT_DTO, CREATE_CONTACT_DTO, CREATE_CONTACT_DTO, NOTES);

    public static final CreateStationManufacturerDto CREATE_STATION_MANUFACTURER_DTO = new CreateStationManufacturerDto(
            MANUFACTURER_NAME, MANUFACTURER_TYPE, VERSION, REMARK);
    public static final CreateTakerServiceManufacturerDto CREATE_TAKER_SERVICE_MANUFACTURER_DTO = new
            CreateTakerServiceManufacturerDto(MANUFACTURER_NAME, SYSTEM_NAME, VERSION, REMARK);
    public static final CreateSupplierDto CREATE_SUPPLIER_DTO = new CreateSupplierDto(STATION_NUMBER, STATION_ID, STATION_NAME,
            STATION_WATER, STATION_WATER_TYPE, CREATE_STATION_MANUFACTURER_DTO, CREATE_CONNECTOR_DTO, REFRESH_RATE, ACCURACY,
            MAIN_USAGE, DATA_CRITICALLY, REFERENCE_LEVEL, REFERENCE_PLACE, WATER_KILOMETER, WATER_SIDE, WATER_LAT,
            WATER_LONG, WATER_LAT, WATER_LONG, HSW, HSW, HSW_REF, HSW, HSW_REF, HSW, HSW_REF, HSW, HSW, HSW, HSW, CHANNEL_USE,
            UTC_IS_USED, IS_SUMMERTIME);
    public static final CreateTakerDto CREATE_TAKER_DTO = new CreateTakerDto(STATION_NUMBER, STATION_ID,
            CREATE_TAKER_SERVICE_MANUFACTURER_DTO, CREATE_CONNECTOR_DTO, REFRESH_RATE);
    //endregion

    //region dto
    public static final ContactDto CONTACT_DTO = new ContactDto(uuid, ORGANIZATION, CONTACT_PERSON,
            CONTACT_STREET, CONTACT_PLZ, LOCATION, CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO,
            EMERGENCY_MAIL, SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO,
            ADMIN_MAIL, NOTES);

    public static final ConnectorDto CONNECTOR_DTO = new ConnectorDto(uuid, CONNECTOR_NUMBER, CONTACT_DTO, DESCRIPTION,
            VERSION, VERSION, DATA_DEFINITION, CONTACT_DTO, CONTACT_DTO, CONTACT_DTO, NOTES);

    public static final StationManufacturerDto STATION_MANUFACTURER_DTO = new StationManufacturerDto(uuid,
            MANUFACTURER_NAME, MANUFACTURER_TYPE, VERSION, REMARK);
    public static final TakerServiceManufacturerDto TAKER_SERVICE_MANUFACTURER_DTO = new
            TakerServiceManufacturerDto(uuid, MANUFACTURER_NAME, SYSTEM_NAME, VERSION, REMARK);
    //endregion
}
