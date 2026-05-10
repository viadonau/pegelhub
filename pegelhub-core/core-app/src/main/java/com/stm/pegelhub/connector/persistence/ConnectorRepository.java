package com.stm.pegelhub.connector.persistence;

import com.stm.pegelhub.connector.domain.Connector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for all {@code Connector}s.
 */
public interface ConnectorRepository {

    /**
     * Saves a connector to the repository.
     *
     * @param connector to save.
     * @return the saved connector.
     */
    Connector saveConnector(Connector connector);

    /**
     * Get a connector from the repository by its id.
     *
     * @param uuid of the connector.
     * @return the found connector.
     */
    Connector getById(UUID uuid);

    /**
     * Get all connectors stored in the repository.
     *
     * @return the found connectors.
     */
    List<Connector> getAllConnectors();

    /**
     * Updates a connector in the repository.
     *
     * @param connector to update.
     * @return the updated connector.
     */
    Connector update(Connector connector);

    /**
     * Deletes a connector by its id.
     *
     * @param uuid of the connector to delete.
     */
    void deleteConnector(UUID uuid);

    /**
     * Returns a connector, if one already exists for this connectorNumber.
     * @param connectorNumber the name of the connector.
     * @return the connector for the given name.
     */
    Optional<Connector> findByConnectorNumber(String connectorNumber);
}
