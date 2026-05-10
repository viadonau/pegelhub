package com.stm.pegelhub.testsupport;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Configuration;

/**
 * Class, which provides automatic configuration for all Jpa Repositories.
 */
@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "com.stm.pegelhub")
@EntityScan(basePackages = "com.stm.pegelhub")
public class JpaIntegrationTestConfiguration {
}
