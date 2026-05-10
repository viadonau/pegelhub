package com.stm.pegelhub.shared.persistence;

import com.stm.pegelhub.auth.domain.*;
import com.stm.pegelhub.connector.domain.*;
import com.stm.pegelhub.contact.domain.*;
import com.stm.pegelhub.supplier.domain.*;
import com.stm.pegelhub.taker.domain.*;
import com.stm.pegelhub.auth.persistence.*;
import com.stm.pegelhub.connector.persistence.*;
import com.stm.pegelhub.contact.persistence.*;
import com.stm.pegelhub.supplier.persistence.*;
import com.stm.pegelhub.taker.persistence.*;

import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of the Interface {@code ApiTokenRepository}.
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
                jpaConnector.getApiToken()
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

    public static ApiToken convert(JpaApiToken token) {
        return new ApiToken(
                token.getId(),
                token.getHashedToken(),
                token.getSalt(),
                token.isActivated(),
                token.getExpiresAt()
        );
    }

    public static Object convert(Object object) {
        if (object instanceof JpaApiToken token) {
            return convert(token);
        } else if (object instanceof JpaContact contact) {
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

    public static <S, T> Optional<S> convert(Optional<T> elem) {
        return (Optional<S>) elem.map(JpaToDomainConverter::convert);
    }

    public static <S, T> List<S> convert(List<T> elements) {
        return (List<S>) elements.stream().map(JpaToDomainConverter::convert).toList();
    }
}
