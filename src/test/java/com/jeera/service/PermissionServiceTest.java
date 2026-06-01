package com.jeera.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;

  @InjectMocks private PermissionService permissionService;

  private static User user(Long id, UserRole role) {
    return User.builder().id(id).username("u" + id).systemRole(role).build();
  }

  private static Project project(Long id, User owner) {
    return Project.builder().id(id).name("p" + id).owner(owner).build();
  }

  @Test
  void isSystemAdmin_handlesNullAndRoles() {
    assertFalse(permissionService.isSystemAdmin(null));
    assertTrue(permissionService.isSystemAdmin(user(1L, UserRole.ADMIN)));
    assertFalse(permissionService.isSystemAdmin(user(1L, UserRole.USER)));
  }

  @Test
  void hasProjectRole_trueWhenMembershipRoleMatches() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.USER)));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, user(2L, UserRole.USER))));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, 1L))
        .thenReturn(Optional.of(ProjectMember.builder().projectRole(ProjectRole.DEVELOPER).build()));

    assertTrue(permissionService.hasProjectRole(1L, 10L, ProjectRole.DEVELOPER, ProjectRole.TESTER));
    assertFalse(permissionService.hasProjectRole(1L, 10L, ProjectRole.TESTER));
  }

  @Test
  void hasProjectRole_throwsWhenUserMissing() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
        () -> permissionService.hasProjectRole(99L, 10L, ProjectRole.DEVELOPER));
  }

  @Test
  void hasProjectRole_throwsWhenMembershipMissing() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.USER)));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, user(2L, UserRole.USER))));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
        () -> permissionService.hasProjectRole(1L, 10L, ProjectRole.DEVELOPER));
  }

  @Test
  void canViewProject_adminAlwaysTrue() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.ADMIN)));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, user(2L, UserRole.USER))));
    assertTrue(permissionService.canViewProject(1L, 10L));
  }

  @Test
  void canViewProject_ownerTrue() {
    User owner = user(1L, UserRole.USER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, owner)));
    assertTrue(permissionService.canViewProject(1L, 10L));
  }

  @Test
  void canViewProject_memberTrue() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, UserRole.USER)));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, user(1L, UserRole.USER))));
    when(projectMemberRepository.existsByProjectIdAndUserId(10L, 3L)).thenReturn(true);
    assertTrue(permissionService.canViewProject(3L, 10L));
  }

  @Test
  void canViewProject_outsiderFalse() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, UserRole.USER)));
    when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, user(1L, UserRole.USER))));
    when(projectMemberRepository.existsByProjectIdAndUserId(10L, 3L)).thenReturn(false);
    assertFalse(permissionService.canViewProject(3L, 10L));
  }
}
