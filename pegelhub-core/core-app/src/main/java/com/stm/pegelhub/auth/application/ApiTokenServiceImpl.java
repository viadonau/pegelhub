package com.stm.pegelhub.auth.application;

import com.stm.pegelhub.auth.domain.ApiToken;
import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.shared.error.NotFoundException;
import com.stm.pegelhub.auth.application.ApiTokenService;
import com.stm.pegelhub.auth.application.Passwords;
import com.stm.pegelhub.auth.persistence.ApiTokenRepository;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of the Interface {@code ApiTokenService}.
 */
@Service
public class ApiTokenServiceImpl implements ApiTokenService {

    private static final int DAYS_TOKEN_LIFETIME = 30;

    private final ApiTokenRepository apiTokenRepository;

    private final ConnectorRepository connectorRepository;

    public ApiTokenServiceImpl(ApiTokenRepository apiTokenRepository, ConnectorRepository connectorRepository) {
        this.apiTokenRepository = requireNonNull(apiTokenRepository);
        this.connectorRepository = connectorRepository;
    }

    /**
     * @param token which ExpirationDate shall be updated
     */
    private void UpdateExpirationDate(ApiToken token) {
        token.setExpiresAt(LocalDateTime.now().plusDays(DAYS_TOKEN_LIFETIME));
    }

    /**
     * @return the newly created Token
     */
    @Override
    public String createToken() {
        String password = Passwords.generateRandomPassword();
        String salt = Passwords.getNextSalt();
        String hashedPassword = Passwords.hash(password, salt);

        ApiToken newToken = new ApiToken();
        newToken.setHashedToken(hashedPassword);
        newToken.setSalt(salt);
        newToken.setActivated(false);
        UpdateExpirationDate(newToken);

        apiTokenRepository.save(newToken);

        return password;
    }

    /**
     * @param apiKey unhashed apiKey of the to be refreshed {@code ApiToken}
     * @return the new password
     */
    @Override
    public String refreshToken(String apiKey, UUID connectorUUID) {

        Connector connector = connectorRepository.getById(connectorUUID);

        Optional<ApiToken> optToken = apiTokenRepository.getById(connector.getApiToken());

        String hashedToken = Passwords.hash(apiKey, optToken.get().getSalt());

        Optional<ApiToken> tokenOpt = apiTokenRepository.getByHashedToken(hashedToken);

        if (tokenOpt.isEmpty()) {
            throw new NotFoundException("token not found");
        }

        ApiToken token = tokenOpt.get();

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("token expired");
        }

        String newPassword = Passwords.generateRandomPassword();
        String newHashedToken = Passwords.hash(newPassword, token.getSalt());
        token.setHashedToken(newHashedToken);
        UpdateExpirationDate(token);

        apiTokenRepository.update(token);

        return newPassword;
    }

    @Override
    public void invalidateToken(String apiKey, UUID uuid) {
        Connector connector = connectorRepository.getById(uuid);

        Optional<ApiToken> optToken = apiTokenRepository.getById(connector.getApiToken());
        String hashedToken = Passwords.hash(apiKey, optToken.get().getSalt());
        Optional<ApiToken> token = apiTokenRepository.getByHashedToken(hashedToken);

        if (token.isEmpty()) {
            throw new NotFoundException("token not found");
        }

        apiTokenRepository.delete(token.get().getId());
    }

    @Override
    public List<UUID> getTokens() {
        return apiTokenRepository.getAll().stream().map(ApiToken::getId).toList();
    }

    @Override
    public void activateToken(UUID uuid) {
        Optional<ApiToken> token = apiTokenRepository.getById(uuid);

        if (token.isEmpty()) {
            throw new NotFoundException("token not found");
        }

        token.get().setActivated(true);

        apiTokenRepository.update(token.get());
    }
}
