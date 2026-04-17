package com.jeera.service;

import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public boolean isSystemAdmin(User user) {
    return user != null && user.getSystemRole() == UserRole.ADMIN;
  }

  public boolean hasProjectRole(Long userId, Long projectId, ProjectRole... allowedRoles) {
    userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

    Set<ProjectRole> allowed = Arrays.stream(allowedRoles).collect(Collectors.toSet());
    ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Project membership not found for user " + userId + " in project " + projectId));

    return allowed.contains(membership.getProjectRole());
  }

  public boolean canViewProject(Long userId, Long projectId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

    if (isSystemAdmin(user)) {
      return true;
    }

    if (project.getOwner().getId().equals(userId)) {
      return true;
    }

    return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
  }
}
