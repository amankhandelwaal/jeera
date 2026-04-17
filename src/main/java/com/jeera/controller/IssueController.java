package com.jeera.controller;

import com.jeera.dto.CreateIssueDto;
import com.jeera.dto.UpdateIssueDto;
import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.service.IssueService;
import com.jeera.service.PermissionService;
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

  @GetMapping("/new")
  public String createIssueForm(
      @PathVariable Long projectId,
      Authentication authentication,
      Model model) {
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

    Issue issue = Issue.builder()
        .project(Project.builder().id(projectId).build())
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
    model.addAttribute("issue", issue);
    model.addAttribute("comments", issue.getComments());
    model.addAttribute("activityLogs", issue.getActivityLogs());
    model.addAttribute("updateIssueDto", new UpdateIssueDto());
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

    issueService.assignDeveloper(issueId, assigneeId, actor.getId());
    redirectAttributes.addFlashAttribute("successMessage", "Issue assigned successfully");
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

    issueService.updateIssueStatus(issueId, updateIssueDto.getStatus(), actor.getId());
    redirectAttributes.addFlashAttribute("successMessage", "Issue status updated successfully");
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
}
