package com.stm.pegelhub.taker.application;

import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.taker.domain.Taker;
import com.stm.pegelhub.taker.application.TakerService;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import com.stm.pegelhub.contact.persistence.ContactRepository;
import com.stm.pegelhub.taker.persistence.TakerRepository;
import com.stm.pegelhub.taker.persistence.TakerServiceManufacturerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.stm.pegelhub.connector.application.ContactUtil.updateConnector;
import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code TakerService}.
 */
@Service
public final class TakerServiceImpl implements TakerService {

    private final TakerRepository takerRepository;
    private final TakerServiceManufacturerRepository takerServiceManufacturerRepository;
    private final ConnectorRepository connectorRepository;
    private final ContactRepository contactRepository;

    public TakerServiceImpl(TakerRepository takerRepository, TakerServiceManufacturerRepository takerServiceManufacturerRepository, ConnectorRepository connectorRepository, ContactRepository contactRepository) {
        this.takerRepository = requireNonNull(takerRepository);
        this.takerServiceManufacturerRepository = requireNonNull(takerServiceManufacturerRepository);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.contactRepository = requireNonNull(contactRepository);
    }

    //TODO: maybe rename to "createTaker" to align this method to the nomenclature of most of the other Service Implementations

    /**
     * @param taker to save.
     * @return the saved {@link Taker}
     */
    @Override
    public Taker saveTaker(Taker taker) {
        Optional<Taker> existingTaker = takerRepository.findByStationNumber(taker.getStationNumber());
        Optional<Connector> existingConnector = connectorRepository.findByConnectorNumber(
                taker.getConnector().getConnectorNumber());
        if (existingTaker.isPresent()) {
            taker = taker.withId(existingTaker.get().getId());
            taker.setTakerServiceManufacturer(taker.getTakerServiceManufacturer()
                    .withId(existingTaker.get().getTakerServiceManufacturer().getId()));
        }
        Connector connector = updateConnector(contactRepository, existingConnector, taker.getConnector());
        taker.setTakerServiceManufacturer(
                takerServiceManufacturerRepository.saveTakerServiceManufacturer(taker.getTakerServiceManufacturer()));
        taker.setConnector(
                connectorRepository.saveConnector(connector));
        return takerRepository.saveTaker(taker);
    }

    /**
     * @param uuid of the taker to be searched for.
     * @return the corresponding {@link Taker} to the specified {@link UUID}
     */
    @Override
    public Taker getTakerById(UUID uuid) {
        return takerRepository.getById(uuid);
    }

    /**
     * @return all saved {@link Taker}s
     */
    @Override
    public List<Taker> getAllTakers() {
        return takerRepository.getAllTakers();
    }

    /**
     * @param uuid {@link UUID} of the taker {@link Taker} to delete.
     */
    @Override
    public void deleteTaker(UUID uuid) {
        takerRepository.deleteTaker(uuid);
    }
}
