package com.jeera.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Starts a single PostgreSQL container for the entire test JVM
 * (the "singleton container" pattern) and wires it into Spring's datasource via
 * {@code @ServiceConnection}.
 *
 * <p>We deliberately do NOT use {@code @Testcontainers}/{@code @Container}: their per-class
 * start/stop lifecycle tears the container down after the first IT class, while Spring's cached
 * application context (shared by every IT with the same configuration) keeps pointing at the now
 * dead container — which surfaces as {@code Connection refused} / HikariCP pool timeouts in every
 * subsequent IT. Starting the container once in a static initializer and letting Testcontainers'
 * Ryuk reaper stop it at JVM shutdown keeps the container alive for the whole cached context.
 *
 * <p>Requires a Docker daemon, so these run in CI (GitHub Actions runners provide Docker) rather
 * than in restricted sandboxes.
 */
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }
}
