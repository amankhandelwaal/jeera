package com.jeera.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSchemaNormalizer implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSchemaNormalizer.class);

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    try {
      jdbcTemplate.execute("ALTER TABLE notifications ALTER COLUMN issue_id DROP NOT NULL");
    } catch (Exception ex) {
      LOGGER.debug("Skipping issue_id nullability update for notifications table", ex);
    }

    try {
      jdbcTemplate.execute("ALTER TABLE notifications ADD COLUMN IF NOT EXISTS project_id BIGINT");
    } catch (Exception ex) {
      LOGGER.debug("Skipping project_id add-column update for notifications table", ex);
    }
  }
}
