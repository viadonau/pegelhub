package com.stm.pegelhub.auth.application;

import com.stm.pegelhub.auth.domain.ApiToken;
import com.stm.pegelhub.shared.error.UnauthorizedException;
import com.stm.pegelhub.auth.application.AuthorizationService;
import com.stm.pegelhub.auth.application.Passwords;
import com.stm.pegelhub.auth.persistence.ApiTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the Interface {@code AuthorizationService}.
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private final ApiTokenRepository apiTokenRepository;

    public AuthorizationServiceImpl(ApiTokenRepository apiTokenRepository) {
        this.apiTokenRepository = apiTokenRepository;
    }

    /**
     * @param apiKey Key to access data.
     * @return the {@link UUID} of the token if it is valid. Else throws an {@link UnauthorizedException}
     */
    @Override
    public UUID authorize(String apiKey) {
        Optional<ApiToken> tokenOpt = null;

        String expectedHash;
        List<ApiToken> tokenList = apiTokenRepository.getAll();

        for(ApiToken t : tokenList)
        {
            expectedHash = Passwords.hash(apiKey, t.getSalt());
            tokenOpt = apiTokenRepository.getByHashedToken(expectedHash);
            if(!tokenOpt.isEmpty())
            {
               break;
            }
        }

        if (tokenOpt.isEmpty()) {
            throw new UnauthorizedException("unauthorized");
        }
        if (!tokenOpt.get().isActivated()) {
            //throw new UnauthorizedException("unauthorized");
        }
        if (tokenOpt.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            //throw new UnauthorizedException("unauthorized");
        }

        return tokenOpt.get().getId();
    }
}
