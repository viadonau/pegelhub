package at.pegelhub.shared.persistence;

import at.pegelhub.auth.domain.ApiToken;
import at.pegelhub.auth.persistence.JpaApiToken;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.JpaContact;
import at.pegelhub.supplier.domain.StationManufacturer;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.supplier.persistence.JpaStationManufacturer;
import at.pegelhub.supplier.persistence.JpaSupplier;
import at.pegelhub.taker.domain.Taker;
import at.pegelhub.taker.domain.TakerServiceManufacturer;
import at.pegelhub.taker.persistence.JpaTaker;
import at.pegelhub.taker.persistence.JpaTakerServiceManufacturer;
import at.pegelhub.auth.application.AuthTokenIdHolder;

import java.util.HashSet;
import java.util.Set;

/**
 * Maps domain classes to their respective JPA classes.
 */

public class DomainToJpaConverter {

    public static JpaContact convert(Contact contact) {
        return new JpaContact(
                contact.getId(),
                contact.getOrganization(),
                contact.getContactPerson(),
                contact.getContactStreet(),
                contact.getContactPlz(),
                contact.getLocation(),
                contact.getContactCountry(),
                contact.getEmergencyNumber(),
                contact.getEmergencyNumberTwo(),
                contact.getEmergencyMail(),
                contact.getServiceNumber(),
                contact.getServiceNumberTwo(),
                contact.getServiceMail(),
                contact.getAdministrationPhoneNumber(),
                contact.getAdministrationPhoneNumberTwo(),
                contact.getAdministrationMail(),
                contact.getContactNodes());
    }

    public static JpaConnector convert(Connector connector) {
        return new JpaConnector(
                connector.getId(),
                connector.getConnectorNumber(),
                convert(connector.getManufacturer()),
                connector.getTypeDescription(),
                connector.getSoftwareVersion(),
                connector.getWorksFromDataVersion(),
                connector.getDataDefinition(),
                convert(connector.getSoftwareManufacturer()),
                convert(connector.getTechnicallyResponsible()),
                convert(connector.getOperationCompany()),
                connector.getNotes(),
                AuthTokenIdHolder.get()

        );
    }

    public static Set<JpaConnector> convert(Set<Connector> connector) {
        Set<JpaConnector> returnValue = new HashSet<>();
        for(Connector c: connector)
        {
            JpaConnector work = new JpaConnector(c.getId(),
                    c.getConnectorNumber(),
                    convert(c.getManufacturer()),
                    c.getTypeDescription(),
                    c.getSoftwareVersion(),
                    c.getWorksFromDataVersion(),
                    c.getDataDefinition(),
                    convert(c.getSoftwareManufacturer()),
                    convert(c.getTechnicallyResponsible()),
                    convert(c.getOperationCompany()),
                    c.getNotes(),
                    AuthTokenIdHolder.get());
            returnValue.add(work);
        }
        return returnValue;
    }

    public static JpaStationManufacturer convert(StationManufacturer stationManufacturer) {
        return new JpaStationManufacturer(
                stationManufacturer.getId(),
                stationManufacturer.getStationManufacturerName(),
                stationManufacturer.getStationManufacturerType(),
                stationManufacturer.getStationManufacturerFirmwareVersion(),
                stationManufacturer.getStationRemark()
        );
    }

    public static JpaSupplier convert(Supplier supplier) {
        return new JpaSupplier(
                supplier.getId(),
                supplier.getStationNumber(),
                supplier.getStationId(),
                supplier.getStationName(),
                supplier.getStationWater(),
                supplier.getStationWaterType(),
                convert(supplier.getStationManufacturer()),
                convert(supplier.getConnector()),
                supplier.getRefreshRate(),
                supplier.getAccuracy(),
                supplier.getMainUsage(),
                supplier.getDataCritically(),
                supplier.getStationBaseReferenceLevel(),
                supplier.getStationReferencePlace(),
                supplier.getStationWaterKilometer(),
                supplier.getStationWaterSide(),
                supplier.getStationWaterLatitude(),
                supplier.getStationWaterLongitude(),
                supplier.getStationWaterLatitudem(),
                supplier.getStationWaterLongitudem(),
                supplier.getHsw100(),
                supplier.getHsw(),
                supplier.getHswReference(),
                supplier.getMw(),
                supplier.getMwReference(),
                supplier.getRnw(),
                supplier.getRnwReference(),
                supplier.getHsq100(),
                supplier.getHsq(),
                supplier.getMq(),
                supplier.getRnq(),
                supplier.getChannelUse(),
                supplier.getUtcIsUsed(),
                supplier.getIsSummertime());
    }

    public static JpaTaker convert(Taker taker) {
        return new JpaTaker(
                taker.getId(),
                taker.getStationNumber(),
                taker.getStationId(),
                convert(taker.getTakerServiceManufacturer()),
                convert(taker.getConnector()),
                taker.getRefreshRate()
        );
    }

    public static JpaApiToken convert(ApiToken token) {
        return new JpaApiToken(
                token.getId(),
                token.getHashedToken(),
                token.getSalt(),
                token.isActivated(),
                token.getExpiresAt()
        );
    }

    public static JpaTakerServiceManufacturer convert(TakerServiceManufacturer takerServiceManufacturer) {
        return new JpaTakerServiceManufacturer(
                takerServiceManufacturer.getId(),
                takerServiceManufacturer.getTakerManufacturerName(),
                takerServiceManufacturer.getTakerSystemName(),
                takerServiceManufacturer.getStationManufacturerFirmwareVersion(),
                takerServiceManufacturer.getRequestRemark()
        );
    }
}
