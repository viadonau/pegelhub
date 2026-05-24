package at.pegelhub.connector.persistence;

import at.pegelhub.shared.persistence.DomainToJpaConverter;
import at.pegelhub.shared.persistence.JpaToDomainConverter;
import at.pegelhub.shared.persistence.*;

import at.pegelhub.connector.domain.Connector;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC Implementation of the Interface {@code ConnectorRepository}.
 */
@Repository
public class JdbcConnectorRepository implements ConnectorRepository {

    private final JpaConnectorRepository jpaConnectorRepository;

    public JdbcConnectorRepository(JpaConnectorRepository jpaConnectorRepository) {
        this.jpaConnectorRepository = jpaConnectorRepository;
    }

    /**
     * @param connector to save.
     * @return the saved {@link Connector}
     */
    @Override
    public Connector saveConnector(Connector connector) {
        if (connector.getId() == null) {
            connector = connector.withId(UUID.randomUUID());
        }
        return JpaToDomainConverter.convert(jpaConnectorRepository.save(DomainToJpaConverter.convert(connector)));
    }

    /**
     * @param uuid {@link UUID} of the {@link Connector} to be searched for.
     * @return the token corresponding to the specified {@link UUID}
     */
    @Override
    public Connector getById(UUID uuid) {
        return jpaConnectorRepository.findById(uuid).map(JpaToDomainConverter::convert).orElse(null);
    }

    /**
     * @return all saved {@link Connector}s
     */
    @Override
    public List<Connector> getAllConnectors() {
        return JpaToDomainConverter.convert(jpaConnectorRepository.findAll());
    }

    /**
     * @param connector {@link Connector} to update.
     * @return the updated {@link Connector}
     */
    @Override
    public Connector update(Connector connector) {
        return JpaToDomainConverter.convert(jpaConnectorRepository.save(DomainToJpaConverter.convert(connector)));
    }

    /**
     * @param uuid of the {@link Connector} to delete.
     */
    @Override
    public void deleteConnector(UUID uuid) {
        jpaConnectorRepository.delete(jpaConnectorRepository.findById(uuid).orElseThrow());
    }

    /**
     * @param connectorNumber the name of the {@link Connector}.
     * @return the {@link Connector} corresponding to the specified connectorNumber
     */
    @Override
    public Optional<Connector> findByConnectorNumber(String connectorNumber) {
        return jpaConnectorRepository.findFirstByConnectorNumber(connectorNumber).map(JpaToDomainConverter::convert);
    }

    @Override
    public Optional<Connector> findByKeycloakClientId(String keycloakClientId) {
        return jpaConnectorRepository.findFirstByKeycloakClientId(keycloakClientId).map(JpaToDomainConverter::convert);
    }
}
