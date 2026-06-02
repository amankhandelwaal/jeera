package com.jeera.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests. Starts a single shared PostgreSQL container (via
 * Testcontainers) and wires it into Spring's datasource with {@code @ServiceConnection}. Requires a
 * Docker daemon, so these run in CI (GitHub Actions runners provide Docker) rather than in
 * restricted sandboxes.
 */
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
