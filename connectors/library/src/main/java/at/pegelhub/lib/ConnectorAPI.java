package at.pegelhub.lib;

import at.pegelhub.lib.model.Connector;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * API spec for connector communication.
 */
public interface ConnectorAPI {
    /**
     * @return a {@code Collection} all known connectors
     */
    Collection<Connector> getConnectors();

    /**
     * @param uuid the identifier of a {@code Connector} entry
     * @return {@code Optional} with {@code Connector} or empty {@code Optional} if none match the {@param uuid}.
     */
    Optional<Connector> getConnectorByUUID(UUID uuid);

    /**
     * Sends a {@code Connector} to the core instance.
     * Throws {@code RuntimeException} if any errors occur.
     * @param connector the {@code Connector} to send
     */
    void sendConnector(Connector connector);
}
