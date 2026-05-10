package com.stm.pegelhub.connector.application;

import com.stm.pegelhub.connector.domain.Connector;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@link Connector}s.
 */
public interface ConnectorService {

    /**
     * Creates a connector.
     *
     * @param connector to save.
     * @return the saved connector.
     */
    Connector createConnector(Connector connector);

    /**
     * Get a connector by its id.
     *
     * @param uuid of the connector.
     * @return the found connector.
     */
    Connector getConnectorById(UUID uuid);

    /**
     * Get all connectors.
     *
     * @return the found connectors.
     */
    List<Connector> getAllConnectors();

    /**
     * Updates a connector.
     *
     * @param connector to update.
     * @return the updated connector.
     */
    Connector updateConnectors(Connector connector);

    /**
     * Deletes a connector by its id.
     *
     * @param uuid of the connector to delete.
     */
    void deleteConnector(UUID uuid);
}
