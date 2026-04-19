package com.jeera.repository;

import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  long countBySystemRole(UserRole systemRole);

  long countBySystemRoleAndIsActiveTrue(UserRole systemRole);

  List<User> findBySystemRole(UserRole systemRole);

  List<User> findBySystemRoleAndIsActiveTrue(UserRole systemRole);

  List<User> findByIsActiveTrueAndSystemRoleNot(UserRole excludedRole);
}
