package com.stm.pegelhub.auth.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.auth.domain.ApiToken;
import com.stm.pegelhub.auth.persistence.JpaApiTokenRepository;
import com.stm.pegelhub.auth.persistence.JdbcApiTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class ApiTokenPostgresIntegrationTest extends JpaIntegrationTestBase {
    private JdbcApiTokenRepository jdbcApiTokenRepository;

    @Autowired
    private JpaApiTokenRepository jpaApiTokenRepository;

    @BeforeEach
    void prepare(){
        jdbcApiTokenRepository = new JdbcApiTokenRepository(jpaApiTokenRepository);
    }

    @Test
    void testSave() {
        Optional<ApiToken> optToken = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));

        ApiToken token = optToken.get().withId(UUID.fromString("950c12a2-c7c2-4224-a1ef-a93dd4cfd76e"));


        jdbcApiTokenRepository.save(token);
        assertEquals(2, jpaApiTokenRepository.findAll().size());

    }
    @Test
    void testGetById() {
        Optional<ApiToken> optToken = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        assertTrue(optToken.isPresent());
    }
    @Test
    void testGetByHashedToken() {
        Optional<ApiToken> optToken = jdbcApiTokenRepository.getByHashedToken("tokenHash");
        assertTrue(optToken.isPresent());
    }
    @Test
    void testDelete() {
        Optional<ApiToken> optToken = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        jdbcApiTokenRepository.delete(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));

        Optional<ApiToken> token = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        assertFalse(token.isPresent());
    }
    @Test
    void testUpdate() {
        Optional<ApiToken> optToken = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        ApiToken token = optToken.get();
        token.setSalt("updatedSalt");
        jdbcApiTokenRepository.update(token);

        Optional<ApiToken> updatedOptToken = jdbcApiTokenRepository.getById(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        ApiToken updatedToken = updatedOptToken.get();

        assertTrue(optToken.isPresent());
        assertEquals(updatedToken.getSalt(), "updatedSalt");
    }
    @Test
    void testGet() {
        assertEquals(1,jdbcApiTokenRepository.getAll().size());
    }
}