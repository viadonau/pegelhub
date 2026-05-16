package at.pegelhub.testsupport;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Configuration;

/**
 * Class, which provides automatic configuration for all Jpa Repositories.
 */
@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "at.pegelhub")
@EntityScan(basePackages = "at.pegelhub")
public class JpaIntegrationTestConfiguration {
}
