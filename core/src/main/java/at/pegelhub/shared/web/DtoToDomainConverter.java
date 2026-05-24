package at.pegelhub.shared.web;

import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.supplier.api.CreateStationManufacturerDto;
import at.pegelhub.supplier.api.CreateSupplierDto;
import at.pegelhub.supplier.domain.StationManufacturer;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.taker.api.CreateTakerDto;
import at.pegelhub.taker.api.CreateTakerServiceManufacturerDto;
import at.pegelhub.taker.domain.Taker;
import at.pegelhub.taker.domain.TakerServiceManufacturer;

import java.time.Duration;

/**
 * Provider, which has methods to turn dtos to domain objects.
 */
public final class DtoToDomainConverter {

    public static Contact convert(CreateContactDto contact) {
        return new Contact(null,
                contact.organization(),
                contact.contactPerson(),
                contact.contactStreet(),
                contact.contactPlz(),
                contact.location(),
                contact.contactCountry(),
                contact.emergencyNumber(),
                contact.emergencyNumberTwo(),
                contact.emergencyMail(),
                contact.serviceNumber(),
                contact.serviceNumberTwo(),
                contact.serviceMail(),
                contact.administrationPhoneNumber(),
                contact.administrationPhoneNumberTwo(),
                contact.administrationMail(),
                contact.contactNodes()
        );
    }

    public static Connector convert(CreateConnectorDto connector) {
        return new Connector(null,
                connector.connectorNumber(),
                convert(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                convert(connector.softwareManufacturer()),
                convert(connector.technicallyResponsible()),
                convert(connector.operationCompany()),
                connector.notes());
    }

    public static StationManufacturer convert(CreateStationManufacturerDto stationManufacturer) {
        return new StationManufacturer(
                null,
                stationManufacturer.stationManufacturerName(),
                stationManufacturer.stationManufacturerType(),
                stationManufacturer.stationManufacturerFirmwareVersion(),
                stationManufacturer.stationRemark()
        );
    }

    public static Supplier convert(CreateSupplierDto supplier) {
        return new Supplier(
                null,
                supplier.stationNumber(),
                supplier.stationId(),
                supplier.stationName(),
                supplier.stationWater(),
                supplier.stationWaterType(),
                convert(supplier.stationManufacturer()),
                convert(supplier.connector()),
                Duration.ofMillis(supplier.refreshRate()),
                supplier.accuracy(),
                supplier.mainUsage(),
                supplier.dataCritically(),
                supplier.stationBaseReferenceLevel(),
                supplier.stationReferencePlace(),
                supplier.stationWaterKilometer(),
                supplier.stationWaterSide(),
                supplier.stationWaterLatitude(),
                supplier.stationWaterLongitude(),
                supplier.stationWaterLatitudem(),
                supplier.stationWaterLongitudem(),
                supplier.hsw100(),
                supplier.hsw(),
                supplier.hswReference(),
                supplier.mw(),
                supplier.mwReference(),
                supplier.rnw(),
                supplier.rnwReference(),
                supplier.hsq100(),
                supplier.hsq(),
                supplier.mq(),
                supplier.rnq(),
                supplier.channelUse(),
                supplier.utcIsUsed(),
                supplier.isSummertime()
        );
    }

        public static Taker convert(CreateTakerDto taker) {
        return new Taker(
                null,
                taker.stationNumber(),
                taker.stationId(),
                convert(taker.takerServiceManufacturer()),
                convert(taker.connector()),
                Duration.ofMillis(taker.refreshRate())
        );
    }

    public static TakerServiceManufacturer convert(CreateTakerServiceManufacturerDto takerServiceManufacturer) {
        return new TakerServiceManufacturer(
                null,
                takerServiceManufacturer.takerManufacturerName(),
                takerServiceManufacturer.takerSystemName(),
                takerServiceManufacturer.stationManufacturerFirmwareVersion(),
                takerServiceManufacturer.requestRemark()
        );
    }
}
