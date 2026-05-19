package at.pegelhub.shared.persistence;

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

import java.util.List;

/**
 * Maps JPA classes to their respective domain classes.
 */

public class JpaToDomainConverter {

    public static Contact convert(JpaContact jpaContact) {
        return new Contact(
                jpaContact.getId(),
                jpaContact.getOrganization(),
                jpaContact.getContactPerson(),
                jpaContact.getContactStreet(),
                jpaContact.getContactPlz(),
                jpaContact.getLocation(),
                jpaContact.getContactCountry(),
                jpaContact.getEmergencyNumber(),
                jpaContact.getEmergencyNumberTwo(),
                jpaContact.getEmergencyMail(),
                jpaContact.getServiceNumber(),
                jpaContact.getServiceNumberTwo(),
                jpaContact.getServiceMail(),
                jpaContact.getAdministrationPhoneNumber(),
                jpaContact.getAdministrationPhoneNumberTwo(),
                jpaContact.getAdministrationMail(),
                jpaContact.getContactNodes());
    }

    public static Connector convert(JpaConnector jpaConnector) {
        return new Connector(
                jpaConnector.getId(),
                jpaConnector.getConnectorNumber(),
                convert(jpaConnector.getManufacturer()),
                jpaConnector.getTypeDescription(),
                jpaConnector.getSoftwareVersion(),
                jpaConnector.getWorksFromDataVersion(),
                jpaConnector.getDataDefinition(),
                convert(jpaConnector.getSoftwareManufacturer()),
                convert(jpaConnector.getTechnicallyResponsible()),
                convert(jpaConnector.getOperatingCompany()),
                jpaConnector.getNodes(),
                jpaConnector.getKeycloakClientId(),
                jpaConnector.getStatus()
        );
    }

    public static StationManufacturer convert(JpaStationManufacturer stationManufacturer) {
        return new StationManufacturer(
                stationManufacturer.getId(),
                stationManufacturer.getStationManufacturerName(),
                stationManufacturer.getStationManufacturerTyp(),
                stationManufacturer.getStationManufacturerFirmwareVersion(),
                stationManufacturer.getStationRemark()
        );
    }

    public static Supplier convert(JpaSupplier supplier) {
        return new Supplier(
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
                supplier.getStationWaterside(),
                supplier.getStationWaterLatitude(),
                supplier.getStationWaterLongitude(),
                supplier.getStationWaterLatitudem(),
                supplier.getStationWaterLongtitudem(),
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
                supplier.getIsSummertime()
        );
    }

    public static Taker convert(JpaTaker taker) {
        return new Taker(
                taker.getId(),
                taker.getStationNumber(),
                taker.getStationId(),
                convert(taker.getTakerServiceManufacturer()),
                convert(taker.getConnector()),
                taker.getRefreshRate()
        );
    }

    public static TakerServiceManufacturer convert(JpaTakerServiceManufacturer takerServiceManufacturer) {
        return new TakerServiceManufacturer(
                takerServiceManufacturer.getId(),
                takerServiceManufacturer.getTakerManufacturerName(),
                takerServiceManufacturer.getTakerSystemName(),
                takerServiceManufacturer.getStationManufacturerFirmwareVersion(),
                takerServiceManufacturer.getRequestRemark()
        );
    }

    public static Object convert(Object object) {
        if (object instanceof JpaContact contact) {
            return convert(contact);
        } else if (object instanceof JpaConnector connector) {
            return convert(connector);
        } else if (object instanceof JpaStationManufacturer stationManufacturer) {
            return convert(stationManufacturer);
        } else if (object instanceof JpaSupplier supplier) {
            return convert(supplier);
        } else if (object instanceof JpaTaker taker) {
            return convert(taker);
        } else if (object instanceof JpaTakerServiceManufacturer takerServiceManufacturer) {
            return convert(takerServiceManufacturer);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <S, T> List<S> convert(List<T> elements) {
        return (List<S>) elements.stream().map(JpaToDomainConverter::convert).toList();
    }
}
