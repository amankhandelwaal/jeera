package com.jeera.service;

import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final IssueRepository issueRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  public Project createProject(Project project, Long creatorUserId) {
    User creator = userRepository.findById(creatorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + creatorUserId));

    project.setOwner(creator);
    project.setCreatedAt(LocalDateTime.now());
    return projectRepository.save(project);
  }

  @Transactional
  public Project createProjectByAdmin(String name, String description, Long ownerId, Long actorUserId) {
    User actor = userRepository.findById(actorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + actorUserId));
    ensureAdmin(actor);

    User owner = resolveEligibleOwner(ownerId);
    Project project = Project.builder()
        .name(normalizeProjectName(name))
        .description(normalizeDescription(description))
        .owner(owner)
        .createdAt(LocalDateTime.now())
        .build();

    Project saved = projectRepository.save(project);
    notificationService.notifyActiveAdmins(
        "Project '" + saved.getName() + "' was created by admin '" + actor.getUsername() + "'.",
        saved);
    return saved;
  }

  @Transactional
  public Project updateProjectByAdmin(Long projectId, String name, String description, Long ownerId, Long actorUserId) {
    User actor = userRepository.findById(actorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + actorUserId));
    ensureAdmin(actor);

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
    User newOwner = resolveEligibleOwner(ownerId);

    project.setName(normalizeProjectName(name));
    project.setDescription(normalizeDescription(description));
    project.setOwner(newOwner);

    Project saved = projectRepository.save(project);
    notificationService.notifyActiveAdmins(
        "Project '" + saved.getName() + "' was updated by admin '" + actor.getUsername() + "'.",
        saved);
    return saved;
  }

  @Transactional
  public Project reassignProjectOwner(Long projectId, Long newOwnerId, Long actorUserId) {
    User actor = userRepository.findById(actorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + actorUserId));
    ensureAdmin(actor);

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
    User newOwner = resolveEligibleOwner(newOwnerId);
    if (project.getOwner() != null && project.getOwner().getId().equals(newOwner.getId())) {
      return project;
    }

    project.setOwner(newOwner);
    Project saved = projectRepository.save(project);
    notificationService.notifyActiveAdmins(
        "PM for project '" + saved.getName() + "' was reassigned to '" + newOwner.getUsername()
            + "' by admin '" + actor.getUsername() + "'.",
        saved);
    return saved;
  }

  @Transactional
  public void deleteProjectByAdmin(Long projectId, Long actorUserId) {
    User actor = userRepository.findById(actorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + actorUserId));
    ensureAdmin(actor);

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

    long issueCount = issueRepository.countByProjectId(projectId);
    if (issueCount > 0) {
      notificationService.notifyActiveAdmins(
          "Project delete blocked for '" + project.getName()
              + "' because it still has issues. Action attempted by admin '"
              + actor.getUsername() + "'.",
          project);
      throw new IllegalStateException("Cannot delete a project that still has issues");
    }

    projectMemberRepository.deleteByProjectId(projectId);
    projectRepository.delete(project);
    notificationService.notifyActiveAdmins(
        "Project '" + project.getName() + "' was deleted by admin '" + actor.getUsername() + "'.");
  }

  public ProjectMember addProjectMember(User user, Project project, ProjectRole projectRole) {
    Project resolvedProject = projectRepository.findById(project.getId())
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + project.getId()));
    User resolvedUser = userRepository.findById(user.getId())
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + user.getId()));

    if (resolvedUser.getSystemRole() == UserRole.ADMIN) {
      throw new IllegalStateException("Admin users cannot be added as project members");
    }

    ProjectMember member = ProjectMember.builder()
        .project(resolvedProject)
        .user(resolvedUser)
        .projectRole(projectRole)
        .addedAt(LocalDateTime.now())
        .build();

    ProjectMember savedMember = projectMemberRepository.save(member);
    notificationService.createProjectNotification(
        resolvedUser,
        "You were added to project '" + resolvedProject.getName() + "' as " + projectRole,
        resolvedProject);

    return savedMember;
  }

  public List<Project> getAllProjects() {
    return projectRepository.findAll();
  }

  public List<User> getEligibleActiveOwners() {
    return userRepository.findByIsActiveTrueAndSystemRoleNot(UserRole.ADMIN);
  }

  public List<ProjectMember> getProjectMembers(Long projectId) {
    return projectMemberRepository.findByProjectId(projectId);
  }

  public List<Project> getOwnedProjects(Long userId) {
    return projectRepository.findByOwnerId(userId);
  }

  public List<Project> getMemberProjects(Long userId) {
    return projectMemberRepository.findByUserId(userId)
        .stream()
        .map(ProjectMember::getProject)
        .toList();
  }

  public List<ProjectMember> getProjectMemberships(Long userId) {
    return projectMemberRepository.findByUserId(userId);
  }

  public List<Project> getAccessibleProjects(Long userId) {
    Map<Long, Project> byId = new LinkedHashMap<>();
    getOwnedProjects(userId).forEach(project -> byId.put(project.getId(), project));
    getMemberProjects(userId).forEach(project -> byId.put(project.getId(), project));
    return byId.values().stream().toList();
  }

  private void ensureAdmin(User actor) {
    if (actor.getSystemRole() != UserRole.ADMIN) {
      throw new IllegalStateException("Only admin can perform this action");
    }
  }

  private User resolveEligibleOwner(Long ownerId) {
    User owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + ownerId));
    if (!owner.isActive()) {
      throw new IllegalStateException("Project owner must be an active user");
    }
    if (owner.getSystemRole() == UserRole.ADMIN) {
      throw new IllegalStateException("Admin users cannot be assigned as PM");
    }
    return owner;
  }

  private String normalizeProjectName(String name) {
    String normalized = name == null ? "" : name.trim();
    if (normalized.isEmpty()) {
      throw new IllegalStateException("Project name is required");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String normalized = description.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
