package com.jeera.controller;

import com.jeera.dto.AddMemberDto;
import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.UserRole;
import com.jeera.service.IssueService;
import com.jeera.service.PermissionService;
import com.jeera.service.ProjectService;
import com.jeera.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;
  private final UserService userService;
  private final IssueService issueService;
  private final PermissionService permissionService;

  @GetMapping({ "", "/" })
  public String projects(Authentication authentication, Model model) {
    User actor = requireActor(authentication);

    if (actor.getSystemRole() == UserRole.ADMIN) {
      return "redirect:/admin/projects";
    }

    List<Project> accessibleProjects = projectService.getAccessibleProjects(actor.getId());
    model.addAttribute("projects", accessibleProjects);
    model.addAttribute("canCreateProject", canCreateProject(actor));
    return "projects/list";
  }

  @GetMapping("/new")
  public String createProjectForm(Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    ensureProjectCreator(actor);

    if (!model.containsAttribute("project")) {
      model.addAttribute("project", new Project());
    }
    return "projects/new";
  }

  @PostMapping("/new")
  public String createProject(
      @Valid Project project,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureProjectCreator(actor);

    if (isBlank(project.getName())) {
      redirectAttributes.addFlashAttribute("errorMessage", "Project name is required");
      redirectAttributes.addFlashAttribute("project", project);
      return "redirect:/projects/new";
    }

    Project created = projectService.createProject(project, actor.getId());
    redirectAttributes.addFlashAttribute("successMessage", "Project created successfully");
    return "redirect:/projects/" + created.getId() + "/issues";
  }

  @GetMapping("/{id}/issues")
  public String projectIssues(@PathVariable Long id, Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), id)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Project project = resolveAccessibleProject(actor, id);
    List<Issue> issues = issueService.findByProjectId(id);
    long open = issues.stream().filter(i -> i.getStatus() == IssueStatus.OPEN).count();
    long unassigned = issues.stream().filter(i -> i.getAssignee() == null).count();
    long inProgress = issues.stream().filter(i -> i.getStatus() == IssueStatus.ASSIGNED
        || i.getStatus() == IssueStatus.IN_ANALYSIS
        || i.getStatus() == IssueStatus.IN_PROGRESS).count();
    long pendingVerify = issues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED
        || i.getStatus() == IssueStatus.UNDER_VERIFICATION).count();
    long closed = issues.stream().filter(i -> i.getStatus() == IssueStatus.CLOSED).count();

    model.addAttribute("project", project);
    model.addAttribute("issues", issues);
    model.addAttribute("canManageMembers", isProjectOwner(actor, project));
    model.addAttribute("openCount", open);
    model.addAttribute("unassignedCount", unassigned);
    model.addAttribute("inProgressCount", inProgress);
    model.addAttribute("pendingVerifyCount", pendingVerify);
    model.addAttribute("closedCount", closed);
    return "projects/detail";
  }

  @GetMapping("/{id}/members")
  public String projectMembers(@PathVariable Long id, Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), id)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Project project = resolveAccessibleProject(actor, id);
    boolean canManageMembers = isProjectOwner(actor, project) || actor.getSystemRole() == UserRole.ADMIN;

    model.addAttribute("project", project);
    model.addAttribute("members", project.getMembers());
    model.addAttribute("addMemberDto", new AddMemberDto());
    model.addAttribute("canManageMembers", canManageMembers);
    return "projects/members";
  }

  @PostMapping("/{id}/members")
  public String addProjectMember(
      @PathVariable Long id,
      @Valid AddMemberDto addMemberDto,
      BindingResult bindingResult,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), id)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Project project = resolveAccessibleProject(actor, id);
    ensureProjectOwnerOrAdmin(actor, project);

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Invalid member data. Please verify username and role.");
      return "redirect:/projects/" + id + "/members";
    }

    User memberUser = userService.findByUsername(addMemberDto.getUsername());
    try {
      projectService.addProjectMember(memberUser, project, addMemberDto.getProjectRole());
      redirectAttributes.addFlashAttribute("successMessage", "Project member added successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/projects/" + id + "/members";
  }

  private User requireActor(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }
    return userService.findByUsername(authentication.getName());
  }

  private Project resolveAccessibleProject(User actor, Long projectId) {
    if (actor.getSystemRole() == UserRole.ADMIN) {
      return projectService.getAllProjects()
          .stream()
          .filter(project -> project.getId().equals(projectId))
          .findFirst()
          .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
    }

    return projectService.getAccessibleProjects(actor.getId())
        .stream()
        .filter(project -> project.getId().equals(projectId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
  }

  private void ensureProjectCreator(User actor) {
    if (!canCreateProject(actor)) {
      throw new AccessDeniedException("Project creation permission required");
    }
  }

  private boolean canCreateProject(User actor) {
    return actor.getSystemRole() == UserRole.ADMIN || actor.isCanCreateProject();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private void ensureProjectOwner(User actor, Project project) {
    if (!isProjectOwner(actor, project)) {
      throw new AccessDeniedException("Unauthorized");
    }
  }

  private void ensureProjectOwnerOrAdmin(User actor, Project project) {
    if (actor.getSystemRole() == UserRole.ADMIN) {
      return;
    }
    ensureProjectOwner(actor, project);
  }

  private boolean isProjectOwner(User actor, Project project) {
    return project.getOwner() != null && project.getOwner().getId().equals(actor.getId());
  }

}
