package com.stm.pegelhub.testsupport;

import org.springframework.test.context.jdbc.Sql;

import java.lang.annotation.*;

/**
 * Class, which specifies the test data for all jpa integration tests.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Sql("/integration-test-data.sql")
public @interface JpaIntegrationTestData {
}
