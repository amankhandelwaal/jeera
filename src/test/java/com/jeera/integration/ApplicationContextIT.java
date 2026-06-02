package com.jeera.integration;

import com.jeera.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the full Spring application context starts and wires every bean (workflow engine,
 * event listeners, security, JPA) against a real PostgreSQL. Runs as an integration test
 * (Testcontainers + Docker) in the verify phase, so the fast unit-test phase needs no database.
 */
@SpringBootTest
class ApplicationContextIT extends AbstractPostgresIT {

  @Test
  void contextLoads() {}
}
