package com.jeera.controller;

import com.jeera.dto.AddMemberDto;
import com.jeera.model.Project;
import com.jeera.model.User;
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

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;
  private final UserService userService;
  private final IssueService issueService;
  private final PermissionService permissionService;

  @GetMapping("/{id}/issues")
  public String projectIssues(@PathVariable Long id, Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), id)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Project project = resolveAccessibleProject(actor, id);
    model.addAttribute("project", project);
    model.addAttribute("issues", issueService.findByProjectId(id));
    return "projects/detail";
  }

  @GetMapping("/{id}/members")
  public String projectMembers(@PathVariable Long id, Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), id)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Project project = resolveAccessibleProject(actor, id);
    ensureProjectOwner(actor, project);

    model.addAttribute("project", project);
    model.addAttribute("members", project.getMembers());
    model.addAttribute("addMemberDto", new AddMemberDto());
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
    ensureProjectOwner(actor, project);

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Invalid member data. Please verify username and role.");
      return "redirect:/projects/" + id + "/members";
    }

    User memberUser = userService.findByUsername(addMemberDto.getUsername());
    projectService.addProjectMember(memberUser, project, addMemberDto.getProjectRole());
    redirectAttributes.addFlashAttribute("successMessage", "Project member added successfully");
    return "redirect:/projects/" + id + "/members";
  }

  private User requireActor(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }
    return userService.findByUsername(authentication.getName());
  }

  private Project resolveAccessibleProject(User actor, Long projectId) {
    return projectService.getAccessibleProjects(actor.getId())
        .stream()
        .filter(project -> project.getId().equals(projectId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
  }

  private void ensureProjectOwner(User actor, Project project) {
    if (project.getOwner() == null || !project.getOwner().getId().equals(actor.getId())) {
      throw new AccessDeniedException("Unauthorized");
    }
  }
}
