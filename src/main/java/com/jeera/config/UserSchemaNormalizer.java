package com.jeera.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSchemaNormalizer implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserSchemaNormalizer.class);

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    try {
      jdbcTemplate.execute(
          "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE");
      jdbcTemplate.execute("UPDATE users SET is_active = TRUE WHERE is_active IS NULL");
      jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN is_active SET NOT NULL");
    } catch (Exception ex) {
      LOGGER.debug("Skipping users.is_active schema normalization", ex);
    }
  }
}
