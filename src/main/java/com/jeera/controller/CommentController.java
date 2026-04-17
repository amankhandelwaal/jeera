package com.jeera.controller;

import com.jeera.model.Issue;
import com.jeera.model.User;
import com.jeera.service.CommentService;
import com.jeera.service.IssueService;
import com.jeera.service.PermissionService;
import com.jeera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/issues/{issueId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;
  private final IssueService issueService;
  private final UserService userService;
  private final PermissionService permissionService;

  @PostMapping("/")
  public String addComment(
      @PathVariable Long projectId,
      @PathVariable Long issueId,
      @RequestParam String body,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User actor = requireActor(authentication);
    if (!permissionService.canViewProject(actor.getId(), projectId)) {
      throw new AccessDeniedException("Unauthorized");
    }

    Issue issue = issueService.findById(issueId);
    if (issue.getProject() == null || !issue.getProject().getId().equals(projectId)) {
      throw new AccessDeniedException("Unauthorized");
    }

    commentService.addComment(issue, actor, body);
    redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully");
    return "redirect:/projects/" + projectId + "/issues/" + issueId;
  }

  private User requireActor(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }
    return userService.findByUsername(authentication.getName());
  }
}
