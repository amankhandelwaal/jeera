package com.jeera.controller;

import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.service.ProjectService;
import com.jeera.service.UserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final ProjectService projectService;

  @GetMapping("login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("register")
  public String publicRegisterForm(Authentication authentication, Model model) {
    if (authentication != null && authentication.isAuthenticated()) {
      return "redirect:/dashboard";
    }

    if (!model.containsAttribute("user")) {
      model.addAttribute("user", new User());
    }
    return "auth/signup";
  }

  @PostMapping("register")
  public String publicRegister(
      @Valid User user,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("user", user);
      return "auth/signup";
    }

    try {
      userService.registerPublicUser(user);
    } catch (IllegalStateException ex) {
      model.addAttribute("user", user);
      model.addAttribute("errorMessage", ex.getMessage());
      return "auth/signup";
    }

    redirectAttributes.addFlashAttribute(
        "successMessage", "Registration successful. Please login with your new account.");
    return "redirect:/login";
  }

  @GetMapping("admin/users")
  public String users(Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    ensureAdmin(actor);

    model.addAttribute("users", userService.getAllUsers());
    model.addAttribute("manageableRoles", List.of(UserRole.USER, UserRole.ADMIN));
    return "admin/users";
  }

  @GetMapping("admin/projects")
  public String adminProjects(
      @RequestParam(required = false) Long ownerId,
      Authentication authentication,
      Model model) {
    User actor = requireActor(authentication);
    ensureAdmin(actor);

    List<ProjectOverview> projectOverviews = projectService.getAllProjects().stream()
        .filter(
            project -> ownerId == null || (project.getOwner() != null && project.getOwner().getId().equals(ownerId)))
        .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
        .map(project -> {
          List<ProjectMember> members = projectService.getProjectMembers(project.getId()).stream()
              .sorted(Comparator.comparing(pm -> pm.getUser().getUsername(), String.CASE_INSENSITIVE_ORDER))
              .toList();
          long developers = members.stream().filter(pm -> pm.getProjectRole() == ProjectRole.DEVELOPER).count();
          long testers = members.stream().filter(pm -> pm.getProjectRole() == ProjectRole.TESTER).count();
          return new ProjectOverview(project, members, developers, testers);
        })
        .toList();

    model.addAttribute("projectOverviews", projectOverviews);
    model.addAttribute("ownerFilterId", ownerId);
    model.addAttribute("eligibleOwners", projectService.getEligibleActiveOwners());
    return "admin/projects";
  }

  @GetMapping("admin/users/new")
  public String registerForm(Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    ensureAdmin(actor);

    model.addAttribute("user", new User());
    return "auth/register";
  }

  @PostMapping("admin/users/new")
  public String registerUserByAdmin(
      @Valid User user,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Authentication authentication,
      Model model) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    if (bindingResult.hasErrors()) {
      model.addAttribute("user", user);
      return "auth/register";
    }

    try {
      userService.registerUserByAdmin(user);
    } catch (IllegalStateException ex) {
      model.addAttribute("user", user);
      model.addAttribute("errorMessage", ex.getMessage());
      return "auth/register";
    }

    redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
    return "redirect:/admin/users";
  }

  @PostMapping("admin/users/{id}/role")
  public String updateUserRole(
      @PathVariable Long id,
      @RequestParam UserRole role,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    if (role != UserRole.USER && role != UserRole.ADMIN) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Only USER and ADMIN are allowed as global system roles");
      return "redirect:/admin/users";
    }

    try {
      userService.updateSystemRole(id, role, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }

    return "redirect:/admin/users";
  }

  @PostMapping("admin/users/{id}/permissions")
  public String updateProjectCreationPermission(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean canCreateProject,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    userService.updateProjectCreationPermission(id, canCreateProject, actor.getId());
    redirectAttributes.addFlashAttribute("successMessage", "Project creation permission updated");
    return "redirect:/admin/users";
  }

  @PostMapping("admin/users/{id}/deactivate")
  public String deactivateUser(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    try {
      userService.deactivateUser(id, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "User deactivated successfully");
      return "redirect:/admin/users";
    } catch (IllegalStateException ex) {
      List<Project> ownedProjects = userService.findOwnedProjects(id);
      if (!ownedProjects.isEmpty()) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        redirectAttributes.addFlashAttribute("ownerFilterId", id);
        redirectAttributes.addFlashAttribute(
            "actionHint",
            "Reassign PM for this user's projects from the Projects page before deactivation.");
        return "redirect:/admin/projects?ownerId=" + id;
      }
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
      return "redirect:/admin/users";
    }
  }

  @PostMapping("admin/projects")
  public String createProjectByAdmin(
      @RequestParam String name,
      @RequestParam(required = false) String description,
      @RequestParam Long ownerId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    try {
      projectService.createProjectByAdmin(name, description, ownerId, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Project created successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("admin/projects/{id}/update")
  public String updateProjectByAdmin(
      @PathVariable Long id,
      @RequestParam String name,
      @RequestParam(required = false) String description,
      @RequestParam Long ownerId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    try {
      projectService.updateProjectByAdmin(id, name, description, ownerId, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Project updated successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("admin/projects/{id}/owner")
  public String reassignProjectOwner(
      @PathVariable Long id,
      @RequestParam Long ownerId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    try {
      projectService.reassignProjectOwner(id, ownerId, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "PM reassigned successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("admin/projects/{id}/delete")
  public String deleteProjectByAdmin(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureAdmin(actor);

    try {
      projectService.deleteProjectByAdmin(id, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Project deleted successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/admin/projects";
  }

  private User requireActor(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }
    return userService.findByUsername(authentication.getName());
  }

  private void ensureAdmin(User actor) {
    if (actor.getSystemRole() != UserRole.ADMIN) {
      throw new AccessDeniedException("Admin access required");
    }
  }

  private record ProjectOverview(
      Project project,
      List<ProjectMember> members,
      long developerCount,
      long testerCount) {
  }
}
