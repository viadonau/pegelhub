package at.pegelhub.taker.persistence;

import at.pegelhub.shared.persistence.DomainToJpaConverter;
import at.pegelhub.shared.persistence.JpaToDomainConverter;
import at.pegelhub.shared.persistence.*;

import at.pegelhub.taker.domain.Taker;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC Implementation of the Interface {@code TakerRepository}.
 */
@Repository
public class JdbcTakerRepository implements TakerRepository {
    private final JpaTakerRepository jpaTakerRepository;

    public JdbcTakerRepository(JpaTakerRepository jpaTakerRepository) {
        this.jpaTakerRepository = jpaTakerRepository;
    }

    /**
     * @param taker to save.
     * @return the saved {@link Taker}
     */
    @Override
    public Taker saveTaker(Taker taker) {
        if (taker.getId() == null) {
            taker = taker.withId(UUID.randomUUID());
        }
        return JpaToDomainConverter.convert(jpaTakerRepository.save(DomainToJpaConverter.convert(taker)));
    }

    /**
     * @param uuid of the desired taker.
     * @return the corresponding {@link Taker} to the given {@link UUID}
     */
    @Override
    public Taker getById(UUID uuid) {
        return jpaTakerRepository.findById(uuid).map(JpaToDomainConverter::convert).orElse(null);
    }

    /**
     * @return all saved {@link Taker}s
     */
    @Override
    public List<Taker> getAllTakers() {
        return JpaToDomainConverter.convert(jpaTakerRepository.findAll());
    }

    /**
     * @param taker to update.
     * @return the updated {@link Taker}
     */
    @Override
    public Taker update(Taker taker) {
        return JpaToDomainConverter.convert(jpaTakerRepository.save(DomainToJpaConverter.convert(taker)));
    }

    /**
     * @param uuid of the taker to delete.
     */
    @Override
    public void deleteTaker(UUID uuid) {
        jpaTakerRepository.delete(jpaTakerRepository.findById(uuid).orElseThrow());
    }

    /**
     * @param stationNumber of the desired {@link Taker}
     * @return the corresponding {@link Taker}(s) to the given stationNumber
     */
    @Override
    public Optional<Taker> findByStationNumber(String stationNumber) {
        return jpaTakerRepository.findFirstByStationNumber(stationNumber).map(JpaToDomainConverter::convert);
    }

    @Override
    public Optional<Taker> findByConnectorKeycloakClientId(String keycloakClientId) {
        return jpaTakerRepository.findByConnectorKeycloakClientId(keycloakClientId).map(JpaToDomainConverter::convert);
    }
}
