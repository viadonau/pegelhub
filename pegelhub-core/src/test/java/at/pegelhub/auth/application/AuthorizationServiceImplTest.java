package at.pegelhub.auth.application;

import at.pegelhub.auth.domain.ApiToken;
import at.pegelhub.auth.persistence.ApiTokenRepository;
import at.pegelhub.shared.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class AuthorizationServiceImplTest {

    private AuthorizationServiceImpl authorizationService;

    private static final ApiTokenRepository REPOSITORY = mock(ApiTokenRepository.class);

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImpl(REPOSITORY);
        reset(REPOSITORY);
    }

    @Test
    void authorizeReturnsTokenIdWhenHashMatches() {
        String apiKey = "valid-api-key";
        ApiToken first = token(UUID.randomUUID(), Passwords.getNextSalt(), true, LocalDateTime.now().plusDays(1));
        ApiToken second = token(UUID.randomUUID(), Passwords.getNextSalt(), true, LocalDateTime.now().plusDays(1));
        String firstHash = Passwords.hash(apiKey, first.getSalt());
        String secondHash = Passwords.hash(apiKey, second.getSalt());

        when(REPOSITORY.getAll()).thenReturn(List.of(first, second));
        when(REPOSITORY.getByHashedToken(firstHash)).thenReturn(Optional.empty());
        when(REPOSITORY.getByHashedToken(secondHash)).thenReturn(Optional.of(second));

        UUID authorizedId = authorizationService.authorize(apiKey);

        assertEquals(second.getId(), authorizedId);
    }

    @Test
    void authorizeThrowsUnauthorizedWhenNoHashMatches() {
        String apiKey = "invalid";
        ApiToken first = token(UUID.randomUUID(), Passwords.getNextSalt(), true, LocalDateTime.now().plusDays(1));
        ApiToken second = token(UUID.randomUUID(), Passwords.getNextSalt(), true, LocalDateTime.now().plusDays(1));

        when(REPOSITORY.getAll()).thenReturn(List.of(first, second));
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, first.getSalt()))).thenReturn(Optional.empty());
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, second.getSalt()))).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authorizationService.authorize(apiKey));
    }

    @Test
    void authorizeThrowsUnauthorizedWhenNoTokensExist() {
        when(REPOSITORY.getAll()).thenReturn(List.of());

        assertThrows(UnauthorizedException.class, () -> authorizationService.authorize("any-key"));
    }

    @Test
    void authorizeDoesNotEnforceActivationYet() {
        String apiKey = "inactive-key";
        ApiToken token = token(UUID.randomUUID(), Passwords.getNextSalt(), false, LocalDateTime.now().plusDays(1));

        when(REPOSITORY.getAll()).thenReturn(List.of(token));
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, token.getSalt()))).thenReturn(Optional.of(token));

        UUID authorizedId = authorizationService.authorize(apiKey);

        assertEquals(token.getId(), authorizedId);
    }

    @Test
    void authorizeDoesNotEnforceExpiryYet() {
        String apiKey = "expired-key";
        ApiToken token = token(UUID.randomUUID(), Passwords.getNextSalt(), true, LocalDateTime.now().minusDays(1));

        when(REPOSITORY.getAll()).thenReturn(List.of(token));
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, token.getSalt()))).thenReturn(Optional.of(token));

        UUID authorizedId = authorizationService.authorize(apiKey);

        assertEquals(token.getId(), authorizedId);
    }

    private static ApiToken token(UUID id, String salt, boolean activated, LocalDateTime expiresAt) {
        ApiToken token = new ApiToken();
        token = token.withId(id);
        token.setSalt(salt);
        token.setHashedToken("hash-" + id);
        token.setActivated(activated);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
