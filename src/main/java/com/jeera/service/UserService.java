package com.jeera.service;

import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final IssueRepository issueRepository;
  private final NotificationService notificationService;
  private final PasswordEncoder passwordEncoder;

  private static final EnumSet<IssueStatus> OPEN_ASSIGNMENT_STATUSES =
      EnumSet.of(
          IssueStatus.REPORTED,
          IssueStatus.OPEN,
          IssueStatus.ASSIGNED,
          IssueStatus.IN_ANALYSIS,
          IssueStatus.IN_PROGRESS,
          IssueStatus.RESOLVED,
          IssueStatus.UNDER_VERIFICATION);

  public User registerPublicUser(User user) {
    user.setSystemRole(resolveDefaultUserRole());
    return createUser(user);
  }

  public User registerUserByAdmin(User user) {
    if (user.getSystemRole() == null) {
      user.setSystemRole(resolveDefaultUserRole());
    } else if (user.getSystemRole() != UserRole.USER && user.getSystemRole() != UserRole.ADMIN) {
      throw new IllegalStateException("Only USER and ADMIN are allowed as global system roles");
    }
    return createUser(user);
  }

  public List<User> getAllUsers() {
    return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  public User findByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new EntityNotFoundException("User not found with username: " + username));
  }

  public List<Project> findOwnedProjects(Long userId) {
    return projectRepository.findByOwnerId(userId);
  }

  public User findById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
  }

  public User updateSystemRole(Long targetUserId, UserRole newRole, Long actorUserId) {
    User actor = findById(actorUserId);
    if (actor.getSystemRole() != UserRole.ADMIN) {
      throw new IllegalStateException("Only admin can update system roles");
    }

    if (newRole != UserRole.USER && newRole != UserRole.ADMIN) {
      throw new IllegalStateException("Only USER and ADMIN are allowed as global system roles");
    }

    User target = findById(targetUserId);
    UserRole currentRole = target.getSystemRole();

    if (currentRole == UserRole.ADMIN && newRole != UserRole.ADMIN) {
      long adminCount = userRepository.countBySystemRole(UserRole.ADMIN);
      if (adminCount <= 1) {
        throw new IllegalStateException("At least one ADMIN must remain in the system");
      }
    }

    target.setSystemRole(newRole);
    return userRepository.save(target);
  }

  public User updateProjectCreationPermission(
      Long targetUserId, boolean canCreateProject, Long actorUserId) {
    User actor = findById(actorUserId);
    if (actor.getSystemRole() != UserRole.ADMIN) {
      throw new IllegalStateException("Only admin can update project creation permission");
    }

    User target = findById(targetUserId);
    target.setCanCreateProject(canCreateProject);
    return userRepository.save(target);
  }

  @Transactional
  public User deactivateUser(Long targetUserId, Long actorUserId) {
    User actor = findById(actorUserId);
    if (actor.getSystemRole() != UserRole.ADMIN) {
      throw new IllegalStateException("Only admin can deactivate users");
    }

    User target = findById(targetUserId);
    if (!target.isActive()) {
      throw new IllegalStateException("User is already deactivated");
    }

    if (target.getSystemRole() == UserRole.ADMIN) {
      long activeAdminCount = userRepository.countBySystemRoleAndIsActiveTrue(UserRole.ADMIN);
      if (activeAdminCount <= 1) {
        throw new IllegalStateException("At least one active ADMIN must remain in the system");
      }
    }

    List<Project> ownedProjects = projectRepository.findByOwnerId(targetUserId);
    if (!ownedProjects.isEmpty()) {
      notificationService.notifyActiveAdmins(
          "User deactivation blocked for '"
              + target.getUsername()
              + "' because they still own projects. Action attempted by admin '"
              + actor.getUsername()
              + "'.");
      String projectSummary =
          ownedProjects.stream()
              .map(project -> "#" + project.getId() + " (" + project.getName() + ")")
              .toList()
              .toString();
      throw new IllegalStateException(
          "Cannot deactivate this user. Reassign PM ownership first for projects: "
              + projectSummary);
    }

    List<Issue> assignedOpenIssues =
        issueRepository.findByAssigneeIdAndStatusIn(targetUserId, OPEN_ASSIGNMENT_STATUSES);
    for (Issue issue : assignedOpenIssues) {
      issue.setAssignee(null);
      issue.setUpdatedAt(LocalDateTime.now());
    }
    issueRepository.saveAll(assignedOpenIssues);

    target.setActive(false);
    User saved = userRepository.save(target);

    notificationService.notifyActiveAdmins(
        "User '"
            + saved.getUsername()
            + "' was deactivated by admin '"
            + actor.getUsername()
            + "'.");
    return saved;
  }

  @Transactional
  public User updateProfile(Long userId, String username, String email) {
    User user = findById(userId);

    String normalizedUsername = username == null ? "" : username.trim();
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

    if (normalizedUsername.isEmpty()) {
      throw new IllegalStateException("Username is required");
    }
    if (normalizedEmail.isEmpty()) {
      throw new IllegalStateException("Email is required");
    }

    userRepository
        .findByUsername(normalizedUsername)
        .filter(existing -> !existing.getId().equals(user.getId()))
        .ifPresent(
            existing -> {
              throw new IllegalStateException("Username is already taken");
            });

    userRepository
        .findByEmail(normalizedEmail)
        .filter(existing -> !existing.getId().equals(user.getId()))
        .ifPresent(
            existing -> {
              throw new IllegalStateException("Email is already in use");
            });

    user.setUsername(normalizedUsername);
    user.setEmail(normalizedEmail);
    return userRepository.save(user);
  }

  private UserRole resolveDefaultUserRole() {
    try {
      return UserRole.valueOf("USER");
    } catch (IllegalArgumentException ex) {
      throw new EntityNotFoundException("Default system role USER is not defined in UserRole enum");
    }
  }

  private User createUser(User user) {
    String username = user.getUsername() == null ? "" : user.getUsername().trim();
    String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();

    if (username.isEmpty()) {
      throw new IllegalStateException("Username is required");
    }
    if (email.isEmpty()) {
      throw new IllegalStateException("Email is required");
    }
    if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
      throw new IllegalStateException("Password is required");
    }

    if (userRepository.existsByUsername(username)) {
      throw new IllegalStateException("Username is already taken");
    }
    if (userRepository.existsByEmail(email)) {
      throw new IllegalStateException("Email is already in use");
    }

    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
    user.setCreatedAt(LocalDateTime.now());
    user.setActive(true);
    return userRepository.save(user);
  }
}
