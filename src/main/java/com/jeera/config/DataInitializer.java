package com.jeera.config;

import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    if (userRepository.count() > 0) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();

    userRepository.save(
        User.builder()
            .username("admin")
            .email("admin@jeera.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .systemRole(UserRole.ADMIN)
            .createdAt(now)
            .build());
  }
}
