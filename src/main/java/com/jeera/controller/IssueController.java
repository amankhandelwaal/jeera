package com.jeera.controller;

import com.jeera.dto.CreateIssueDto;
import com.jeera.dto.UpdateIssueDto;
import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.service.ActivityLogService;
import com.jeera.service.CommentService;
import com.jeera.service.IssueService;
import com.jeera.service.PermissionService;
import com.jeera.service.ProjectService;
import com.jeera.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/issues")
@RequiredArgsConstructor
public class IssueController {

  private final IssueService issueService;
  private final UserService userService;
  private final PermissionService permissionService;
  private final ProjectService projectService;
  private final CommentService commentService;
  private final ActivityLogService activityLogService;

  @GetMapping("/new")
  public String createIssueForm(
      @PathVariable Long projectId, Authentication authentication, Model model) {
    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);

    CreateIssueDto dto = new CreateIssueDto();
    dto.setProjectId(projectId);
    model.addAttribute("createIssueDto", dto);
    return "issues/create";
  }

  @PostMapping("/new")
  public String createIssue(
      @PathVariable Long projectId,
      @Valid @ModelAttribute("createIssueDto") CreateIssueDto createIssueDto,
      BindingResult bindingResult,
      Authentication authentication,
      RedirectAttributes redirectAttributes,
      Model model) {

    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);

    if (bindingResult.hasErrors()) {
      model.addAttribute("createIssueDto", createIssueDto);
      return "issues/create";
    }

    Project project =
        projectService.getAccessibleProjects(actor.getId()).stream()
            .filter(p -> p.getId().equals(projectId))
            .findFirst()
            .orElseThrow(
                () -> new EntityNotFoundException("Project not found with id: " + projectId));

    Issue issue =
        Issue.builder()
            .project(project)
            .title(createIssueDto.getTitle())
            .description(createIssueDto.getDescription())
            .type(createIssueDto.getType())
            .priority(createIssueDto.getPriority())
            .build();

    issueService.createIssue(issue, actor.getId());
    redirectAttributes.addFlashAttribute("successMessage", "Issue created successfully");
    return "redirect:/projects/" + projectId + "/issues";
  }

  @GetMapping("/{issueId}")
  public String issueDetail(
      @PathVariable Long projectId,
      @PathVariable Long issueId,
      Authentication authentication,
      Model model) {
    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);

    Issue issue = issueService.findById(issueId);
    ensureIssueBelongsToProject(issue, projectId);

    boolean isProjectOwner =
        issue.getProject() != null
            && issue.getProject().getOwner() != null
            && issue.getProject().getOwner().getId().equals(actor.getId());
    boolean isAdmin = actor.getSystemRole() == UserRole.ADMIN;
    boolean canManageIssue = isProjectOwner || isAdmin;
    boolean canUpdateAsAssignee =
        issue.getAssignee() != null && issue.getAssignee().getId().equals(actor.getId());
    boolean canVerifyAsTester = hasProjectRole(actor, projectId, ProjectRole.TESTER);
    List<User> assignableDevelopers =
        canManageIssue ? issueService.getAssignableDevelopers(projectId) : List.of();

    model.addAttribute("issue", issue);
    model.addAttribute("comments", commentService.getIssueComments(issueId));
    model.addAttribute("activityLogs", activityLogService.getIssueActivityLogs(issueId));
    model.addAttribute("updateIssueDto", new UpdateIssueDto());
    model.addAttribute("canManageIssue", canManageIssue);
    model.addAttribute("canUpdateAsAssignee", canUpdateAsAssignee);
    model.addAttribute("canVerifyAsTester", canVerifyAsTester);
    model.addAttribute("assignableDevelopers", assignableDevelopers);
    return "issues/detail";
  }

  @PostMapping("/{issueId}/assign")
  public String assignDeveloper(
      @PathVariable Long projectId,
      @PathVariable Long issueId,
      @RequestParam Long assigneeId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);
    Issue issue = issueService.findById(issueId);
    ensureIssueBelongsToProject(issue, projectId);
    ensureProjectOwner(actor, issue);

    try {
      issueService.assignDeveloper(issueId, assigneeId, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Issue assigned successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/projects/" + projectId + "/issues/" + issueId;
  }

  @PostMapping("/{issueId}/pickup")
  public String pickUpForVerification(
      @PathVariable Long projectId,
      @PathVariable Long issueId,
      @RequestParam(name = "redirect", defaultValue = "issue") String redirectTarget,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);

    try {
      issueService.pickUpForVerification(issueId, actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Issue picked for verification");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }

    if ("dashboard".equalsIgnoreCase(redirectTarget)) {
      return "redirect:/dashboard?view=tester";
    }
    return "redirect:/projects/" + projectId + "/issues/" + issueId;
  }

  @PostMapping("/{issueId}/status")
  public String updateStatus(
      @PathVariable Long projectId,
      @PathVariable Long issueId,
      @Valid @ModelAttribute("updateIssueDto") UpdateIssueDto updateIssueDto,
      BindingResult bindingResult,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    ensureProjectMember(actor, projectId);
    Issue issue = issueService.findById(issueId);
    ensureIssueBelongsToProject(issue, projectId);

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Invalid status update request");
      return "redirect:/projects/" + projectId + "/issues/" + issueId;
    }

    try {
      issueService.updateIssueStatus(issueId, updateIssueDto.getStatus(), actor.getId());
      redirectAttributes.addFlashAttribute("successMessage", "Issue status updated successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/projects/" + projectId + "/issues/" + issueId;
  }

  private User requireActor(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }
    return userService.findByUsername(authentication.getName());
  }

  private void ensureProjectMember(User actor, Long projectId) {
    if (!permissionService.canViewProject(actor.getId(), projectId)) {
      throw new AccessDeniedException("Unauthorized");
    }
  }

  private void ensureProjectOwner(User actor, Issue issue) {
    if (issue.getProject() == null
        || issue.getProject().getOwner() == null
        || !issue.getProject().getOwner().getId().equals(actor.getId())) {
      throw new AccessDeniedException("Unauthorized");
    }
  }

  private void ensureIssueBelongsToProject(Issue issue, Long projectId) {
    if (issue.getProject() == null || !issue.getProject().getId().equals(projectId)) {
      throw new EntityNotFoundException("Issue not found with id: " + issue.getId());
    }
  }

  private boolean hasProjectRole(User actor, Long projectId, ProjectRole role) {
    try {
      return permissionService.hasProjectRole(actor.getId(), projectId, role);
    } catch (EntityNotFoundException ex) {
      return false;
    }
  }
}
