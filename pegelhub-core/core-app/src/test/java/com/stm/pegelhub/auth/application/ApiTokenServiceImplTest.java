package com.stm.pegelhub.auth.application;

import com.stm.pegelhub.auth.domain.ApiToken;
import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.shared.error.NotFoundException;
import com.stm.pegelhub.auth.application.Passwords;
import com.stm.pegelhub.auth.persistence.ApiTokenRepository;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ApiTokenServiceImplTest {

    private ApiTokenServiceImpl sut;
    private static final ApiTokenRepository REPOSITORY = mock(ApiTokenRepository.class);
    private static final ConnectorRepository CONNECTOR_REPOSITORY = mock(ConnectorRepository.class);

    @BeforeEach
    void setUp() {
        sut = new ApiTokenServiceImpl(REPOSITORY, CONNECTOR_REPOSITORY);
        reset(REPOSITORY);
        reset(CONNECTOR_REPOSITORY);
    }

    @Test
    public void constructorShouldThrowNullPointerExceptionIfApiTokenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new ApiTokenServiceImpl(null, null));
    }

    @Test
    void testCreateToken() {
        String password = sut.createToken();
        assertNotNull(password);
    }

    @Test
    void testRefreshTokenWithValidToken() {
        String apiKey = "valid_token";
        UUID connectorUuid = UUID.randomUUID();
        UUID tokenUuid = UUID.randomUUID();
        String salt = Passwords.getNextSalt();
        ApiToken apiToken = new ApiToken();
        apiToken = apiToken.withId(tokenUuid);
        apiToken.setHashedToken(Passwords.hash(apiKey, salt));
        apiToken.setSalt(salt);
        apiToken.setActivated(true);
        apiToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        Connector connector = new Connector();
        connector.setApiToken(tokenUuid);

        when(CONNECTOR_REPOSITORY.getById(connectorUuid)).thenReturn(connector);
        when(REPOSITORY.getById(tokenUuid)).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.getByHashedToken(apiToken.getHashedToken())).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.update(apiToken)).thenReturn(apiToken);

        String newToken = sut.refreshToken(apiKey, connectorUuid);

        assertNotNull(newToken);
    }

    @Test
    void testRefreshTokenWithExpiredToken() {
        String apiKey = "expired_token";
        UUID connectorUuid = UUID.randomUUID();
        UUID tokenUuid = UUID.randomUUID();
        String salt = Passwords.getNextSalt();
        ApiToken apiToken = new ApiToken();
        apiToken = apiToken.withId(tokenUuid);
        apiToken.setHashedToken(Passwords.hash(apiKey, salt));
        apiToken.setSalt(salt);
        apiToken.setActivated(true);
        apiToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        Connector connector = new Connector();
        connector.setApiToken(tokenUuid);

        when(CONNECTOR_REPOSITORY.getById(connectorUuid)).thenReturn(connector);
        when(REPOSITORY.getById(tokenUuid)).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.getByHashedToken(apiToken.getHashedToken())).thenReturn(Optional.of(apiToken));

        Assertions.assertThrows(IllegalArgumentException.class, () -> sut.refreshToken(apiKey, connectorUuid));
    }

    @Test
    void testRefreshTokenWithInvalidToken() {
        String apiKey = "invalid_token";
        UUID connectorUuid = UUID.randomUUID();
        UUID tokenUuid = UUID.randomUUID();
        String salt = "salt";
        ApiToken apiToken = new ApiToken().withId(tokenUuid);
        apiToken.setSalt(salt);
        Connector connector = new Connector();
        connector.setApiToken(tokenUuid);

        when(CONNECTOR_REPOSITORY.getById(connectorUuid)).thenReturn(connector);
        when(REPOSITORY.getById(tokenUuid)).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, salt))).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> sut.refreshToken(apiKey, connectorUuid));
    }

    @Test
    void testInvalidateTokenWithValidToken() {
        String apiKey = "valid_token";
        UUID connectorUuid = UUID.randomUUID();
        UUID tokenUuid = UUID.randomUUID();
        String salt = Passwords.getNextSalt();
        ApiToken apiToken = new ApiToken();
        apiToken = apiToken.withId(tokenUuid);
        apiToken.setHashedToken(Passwords.hash(apiKey, salt));
        apiToken.setSalt(salt);
        apiToken.setExpiresAt(LocalDateTime.now().plusDays(3));
        Connector connector = new Connector();
        connector.setApiToken(tokenUuid);

        when(CONNECTOR_REPOSITORY.getById(connectorUuid)).thenReturn(connector);
        when(REPOSITORY.getById(tokenUuid)).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.getByHashedToken(apiToken.getHashedToken())).thenReturn(Optional.of(apiToken));

        sut.invalidateToken(apiKey, connectorUuid);

        Mockito.verify(REPOSITORY, Mockito.times(1)).delete(apiToken.getId());
    }

    @Test
    void testInvalidateTokenWithInvalidToken() {
        String apiKey = "invalid_token";
        UUID connectorUuid = UUID.randomUUID();
        UUID tokenUuid = UUID.randomUUID();
        String salt = "salt";
        ApiToken apiToken = new ApiToken().withId(tokenUuid);
        apiToken.setSalt(salt);
        Connector connector = new Connector();
        connector.setApiToken(tokenUuid);

        when(CONNECTOR_REPOSITORY.getById(connectorUuid)).thenReturn(connector);
        when(REPOSITORY.getById(tokenUuid)).thenReturn(Optional.of(apiToken));
        when(REPOSITORY.getByHashedToken(Passwords.hash(apiKey, salt))).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> sut.invalidateToken(apiKey, connectorUuid));
    }

    @Test
    public void testGetTokens() {
        List<ApiToken> tokens = Arrays.asList(
                new ApiToken().withId(UUID.randomUUID()),
                new ApiToken().withId(UUID.randomUUID())
        );
        when(REPOSITORY.getAll()).thenReturn(tokens);

        List<UUID> result = sut.getTokens();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(tokens.get(0).getId(), result.get(0));
        assertEquals(tokens.get(1).getId(), result.get(1));
    }

    @Test
    public void testActivateToken() {
        UUID tokenId = UUID.randomUUID();
        ApiToken token = new ApiToken().withId(tokenId);
        when(REPOSITORY.getById(tokenId)).thenReturn(Optional.of(token));

        sut.activateToken(tokenId);

        assertTrue(token.isActivated());
        verify(REPOSITORY).update(token);
    }

    @Test
    public void testActivateTokenWithUnknownToken() {
        UUID tokenId = UUID.randomUUID();
        when(REPOSITORY.getById(tokenId)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> sut.activateToken(tokenId));

    }
}
