package com.jeera.service;

import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  public Project createProject(Project project, Long creatorUserId) {
    User creator = userRepository.findById(creatorUserId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + creatorUserId));

    project.setOwner(creator);
    project.setCreatedAt(LocalDateTime.now());
    return projectRepository.save(project);
  }

  public ProjectMember addProjectMember(User user, Project project, ProjectRole projectRole) {
    Project resolvedProject = projectRepository.findById(project.getId())
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + project.getId()));
    User resolvedUser = userRepository.findById(user.getId())
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + user.getId()));

    ProjectMember member = ProjectMember.builder()
        .project(resolvedProject)
        .user(resolvedUser)
        .projectRole(projectRole)
        .addedAt(LocalDateTime.now())
        .build();

    return projectMemberRepository.save(member);
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

  public List<Project> getAccessibleProjects(Long userId) {
    Map<Long, Project> byId = new LinkedHashMap<>();
    getOwnedProjects(userId).forEach(project -> byId.put(project.getId(), project));
    getMemberProjects(userId).forEach(project -> byId.put(project.getId(), project));
    return byId.values().stream().toList();
  }
}
