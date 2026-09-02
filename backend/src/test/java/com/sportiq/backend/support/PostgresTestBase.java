package com.sportiq.backend.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for every test that needs a real PostgreSQL instance
 * (repository, service and controller integration tests). Tests are
 * skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresTestBase {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}