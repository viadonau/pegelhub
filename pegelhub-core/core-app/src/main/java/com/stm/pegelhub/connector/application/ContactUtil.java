package com.stm.pegelhub.connector.application;


import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.contact.persistence.ContactRepository;

import java.util.Optional;

/**
 * Util class for connector creation things.
 */
public class ContactUtil {

    //TODO: figure out what "existingConnector" should be
    /**
     * @param contactRepository the {@link ContactRepository} that is to fetch data from.
     * @param existingConnector the former Connector which shall be replaced
     * @param connector the Connector from the Supplier/Taker
     * @return a new {@link Connector}
     */
    public static final Connector updateConnector(ContactRepository contactRepository, Optional<Connector> existingConnector, Connector connector) {
        if (existingConnector.isPresent()) {
            /*
             * if supplier/taker already has a Connector (existingConnector) in use,
             * existingConnector is assigned to oldConnector.
             * existingConnector supplies the UUID for connector
             */
            Connector oldConnector = existingConnector.get();
            connector = connector.withId(oldConnector.getId());
            /*
             * if oldConnector has a Manufacturer assigned to it, connector gets a new
             * Contact object (manufacturer) assigned to it. The new Manufacturer is
             * assigned the UUID of oldConnector, the other properties are fetched from the former Contact.
             * If oldConnector has no Manufacturer, then a new Contact object (Manufacturer) will
             * be created with a random UUID
             * If connector has no Manufacturer, it will not receive one
             */
            if (connector.getManufacturer() != null) {
                if (oldConnector.getManufacturer() != null) {
                    connector.setManufacturer(
                            connector.getManufacturer().withId(oldConnector.getManufacturer().getId()));
                }
                connector.setManufacturer(contactRepository.saveContact(connector.getManufacturer()));
            }
            /*
             * if oldConnector has an OperationCompany assigned to it, connector gets a new
             * Contact object (OperationCompany) assigned to it. The new OperationCompany is
             * assigned the UUID of oldConnector, the other properties are fetched from the former Contact.
             * If oldConnector has no OperationCompany, then a new Contact object (OperationCompany) will
             * be created with a random UUID
             * If connector has no OperationCompany, it will not receive one
             */
            if (connector.getOperationCompany() != null) {
                if (oldConnector.getOperationCompany() != null) {
                    connector.setOperationCompany(
                            connector.getOperationCompany().withId(oldConnector.getOperationCompany().getId()));
                }
                connector.setOperationCompany(contactRepository.saveContact(connector.getOperationCompany()));
            }
            /*
             * if oldConnector has an SoftwareManufacturer assigned to it, connector gets a new
             * Contact object (SoftwareManufacturer) assigned to it. The new SoftwareManufacturer is
             * assigned the UUID of oldConnector, the other properties are fetched from the former Contact.
             * If oldConnector has no SoftwareManufacturer, then a new Contact object (SoftwareManufacturer) will
             * be created with a random UUID
             * If connector has no SoftwareManufacturer, it will not receive one
             */
            if (connector.getSoftwareManufacturer() != null) {
                if (oldConnector.getSoftwareManufacturer() != null) {
                    connector.setSoftwareManufacturer(
                            connector.getSoftwareManufacturer().withId(oldConnector.getSoftwareManufacturer().getId()));
                }
                connector.setSoftwareManufacturer(contactRepository.saveContact(connector.getSoftwareManufacturer()));
            }
            /*
             * if oldConnector has an TechnicallyResponsible assigned to it, connector gets a new
             * Contact object (TechnicallyResponsible) assigned to it. The new TechnicallyResponsible is
             * assigned the UUID of oldConnector, the other properties are fetched from the former Contact.
             * If oldConnector has no TechnicallyResponsible, then a new Contact object (TechnicallyResponsible) will
             * be created with a random UUID
             * If connector has no TechnicallyResponsible, it will not receive one
             */
            if (connector.getTechnicallyResponsible() != null) {
                if (oldConnector.getTechnicallyResponsible() != null) {
                    connector.setTechnicallyResponsible(
                            connector.getTechnicallyResponsible().withId(oldConnector.getTechnicallyResponsible().getId()));
                }
                connector.setTechnicallyResponsible(contactRepository.saveContact(connector.getTechnicallyResponsible()));
            }
            /*
             * If existingConnector is null (does not exist), connector gets new Manufacturer object, OperationCompany,
             * SoftwareManufacturer and TechnicallyResponsible if those Objects exist in connector. These newly created
             * Objects get the properties of the former Objects in connector, but a new, randomly created, UUID.
             */
        } else {
            if (connector.getManufacturer() != null) {
                connector.setManufacturer(contactRepository.saveContact(connector.getManufacturer()));
            }
            if (connector.getOperationCompany() != null) {
                connector.setOperationCompany(contactRepository.saveContact(connector.getOperationCompany()));
            }
            if (connector.getSoftwareManufacturer() != null) {
                connector.setSoftwareManufacturer(contactRepository.saveContact(connector.getSoftwareManufacturer()));
            }
            if (connector.getTechnicallyResponsible() != null) {
                connector.setTechnicallyResponsible(contactRepository.saveContact(connector.getTechnicallyResponsible()));
            }
        }
        return connector;
    }

}
