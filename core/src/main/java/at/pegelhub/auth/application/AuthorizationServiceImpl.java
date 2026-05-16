package at.pegelhub.auth.application;

import at.pegelhub.auth.domain.ApiToken;
import at.pegelhub.shared.error.UnauthorizedException;
import at.pegelhub.auth.persistence.ApiTokenRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the Interface {@code AuthorizationService}.
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

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
        Optional<ApiToken> tokenOpt = Optional.empty();

        String expectedHash;
        List<ApiToken> tokenList = apiTokenRepository.getAll();

        for(ApiToken t : tokenList)
        {
            expectedHash = Passwords.hash(apiKey, t.getSalt());
            tokenOpt = apiTokenRepository.getByHashedToken(expectedHash);
            if (tokenOpt.isPresent())
            {
               break;
            }
        }

        if (tokenOpt.isEmpty()) {
            throw new UnauthorizedException("unauthorized");
        }

        // TODO Activate Token checking with spring security
//        if (!tokenOpt.get().isActivated()) {
//            //throw new UnauthorizedException("unauthorized");
//        }
//        if (tokenOpt.get().getExpiresAt().isBefore(LocalDateTime.now())) {
//            //throw new UnauthorizedException("unauthorized");
//        }

        return tokenOpt.get().getId();
    }
}
