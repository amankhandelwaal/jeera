package com.jeera.service;

import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public User registerUser(User user) {
    user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
    user.setSystemRole(resolveDefaultUserRole());
    user.setCreatedAt(LocalDateTime.now());
    return userRepository.save(user);
  }

  public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
  }

  public User findById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
  }

  private UserRole resolveDefaultUserRole() {
    try {
      return UserRole.valueOf("USER");
    } catch (IllegalArgumentException ex) {
      throw new EntityNotFoundException("Default system role USER is not defined in UserRole enum");
    }
  }
}
