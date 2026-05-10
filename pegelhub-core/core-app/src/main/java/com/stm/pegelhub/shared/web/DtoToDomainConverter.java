package com.stm.pegelhub.shared.web;

import com.stm.pegelhub.auth.domain.*;
import com.stm.pegelhub.connector.domain.*;
import com.stm.pegelhub.contact.domain.*;
import com.stm.pegelhub.supplier.domain.*;
import com.stm.pegelhub.taker.domain.*;
import com.stm.pegelhub.auth.api.*;
import com.stm.pegelhub.connector.api.*;
import com.stm.pegelhub.contact.api.*;
import com.stm.pegelhub.supplier.api.*;
import com.stm.pegelhub.taker.api.*;

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
                connector.notes(), connector.apiToken());
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
