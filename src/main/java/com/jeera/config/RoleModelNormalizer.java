package com.jeera.config;

import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleModelNormalizer implements CommandLineRunner {

  private final UserRepository userRepository;

  @Override
  public void run(String... args) {
    List<User> users = userRepository.findAll();
    boolean changed = false;

    for (User user : users) {
      UserRole role = user.getSystemRole();
      if (role == UserRole.PM || role == UserRole.DEVELOPER || role == UserRole.TESTER) {
        user.setSystemRole(UserRole.USER);

        // Preserve previous PM project-creation behavior while moving to explicit
        // permission model.
        if (role == UserRole.PM) {
          user.setCanCreateProject(true);
        }

        changed = true;
      }
    }

    if (changed) {
      userRepository.saveAll(users);
    }
  }
}
