package at.pegelhub.auth.persistence;

import at.pegelhub.shared.persistence.DomainToJpaConverter;
import at.pegelhub.shared.persistence.JpaToDomainConverter;
import at.pegelhub.shared.persistence.*;

import at.pegelhub.auth.domain.ApiToken;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC Implementation of the Interface {@code ApiTokenRepository}.
 */
@Repository
public class JdbcApiTokenRepository implements ApiTokenRepository {
    private final JpaApiTokenRepository jpaApiTokenRepository;


    public JdbcApiTokenRepository(JpaApiTokenRepository jpaApiTokenRepository) {
        this.jpaApiTokenRepository = jpaApiTokenRepository;
    }

    /**
     * @param token {@code ApiToken} to add.
     * @return the saved token.
     */
    @Override
    public ApiToken save(ApiToken token) {
        if (token.getId() == null) {
            token = token.withId(UUID.randomUUID());
        }
        return JpaToDomainConverter.convert(jpaApiTokenRepository.save(DomainToJpaConverter.convert(token)));
    }

    /**
     * @param uuid {@code UUID} of the {@code ApiToken} which to search for
     * @return the corresponding token to the specified UUID
     */
    @Override
    public Optional<ApiToken> getById(UUID uuid) {
        return jpaApiTokenRepository.findById(uuid).map(JpaToDomainConverter::convert);
    }

    /**
     * @param hashedToken hashed {@code ApiToken}
     * @return the token corresponding to the specified hashedToken
     */
    @Override
    public Optional<ApiToken> getByHashedToken(String hashedToken) {
        return jpaApiTokenRepository.getByHashedToken(hashedToken).map(JpaToDomainConverter::convert);
    }

    /**
     * @param uuid {@code UUID} of the {@code ApiToken} to be deleted
     */
    @Override
    public void delete(UUID uuid) {
        jpaApiTokenRepository.deleteById(uuid);
    }

    /**
     * @param token {@code ApiToken} to update.
     * @return the updated token.
     */
    @Override
    public ApiToken update(ApiToken token) {
        return JpaToDomainConverter.convert(jpaApiTokenRepository.save(DomainToJpaConverter.convert(token)));
    }

    /**
     * @return all saved tokens
     */
    @Override
    public List<ApiToken> getAll() {
        return JpaToDomainConverter.convert(jpaApiTokenRepository.findAll());
    }

}
