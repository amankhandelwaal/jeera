package com.jeera.controller;

import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.service.IssueService;
import com.jeera.service.ProjectService;
import com.jeera.service.UserService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final UserService userService;
  private final ProjectService projectService;
  private final IssueService issueService;

  @GetMapping
  public String dashboard(
      Authentication authentication,
      @RequestParam(name = "view", defaultValue = "projects") String view,
      Model model) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return "redirect:/login";
    }

    User user = userService.findByUsername(authentication.getName());
    if (user.getSystemRole() == UserRole.ADMIN) {
      return "redirect:/projects";
    }

    List<Project> ownedProjects = projectService.getOwnedProjects(user.getId());
    List<Project> memberProjects = projectService.getMemberProjects(user.getId());
    List<ProjectMember> memberships = projectService.getProjectMemberships(user.getId());

    Map<Long, Project> accessible = new LinkedHashMap<>();
    ownedProjects.forEach(project -> accessible.put(project.getId(), project));
    memberProjects.forEach(project -> accessible.put(project.getId(), project));
    List<Project> accessibleProjects = new ArrayList<>(accessible.values());

    List<Issue> projectIssues =
        accessibleProjects.stream()
            .flatMap(project -> issueService.findByProjectId(project.getId()).stream())
            .toList();

    List<Issue> myAssignedIssues =
        projectIssues.stream()
            .filter(
                issue ->
                    issue.getAssignee() != null && issue.getAssignee().getId().equals(user.getId()))
            .toList();

    List<Issue> assigned = filterByStatus(myAssignedIssues, "ASSIGNED");
    List<Issue> inAnalysis = filterByStatus(myAssignedIssues, "IN_ANALYSIS");
    List<Issue> inProgress = filterByStatus(myAssignedIssues, "IN_PROGRESS");
    List<Issue> resolved = filterByStatus(myAssignedIssues, "RESOLVED");

    Set<Long> testerProjectIds =
        memberships.stream()
            .filter(member -> member.getProjectRole() == ProjectRole.TESTER)
            .map(member -> member.getProject().getId())
            .collect(Collectors.toCollection(HashSet::new));

    List<Issue> pendingVerification =
        projectIssues.stream()
            .filter(
                issue ->
                    issue.getProject() != null
                        && testerProjectIds.contains(issue.getProject().getId()))
            .filter(issue -> issue.getStatus().name().equals("RESOLVED"))
            .toList();

    List<Issue> myActiveVerifications =
        projectIssues.stream()
            .filter(
                issue ->
                    issue.getProject() != null
                        && testerProjectIds.contains(issue.getProject().getId()))
            .filter(issue -> issue.getStatus().name().equals("UNDER_VERIFICATION"))
            .filter(
                issue ->
                    issue.getAssignee() != null && issue.getAssignee().getId().equals(user.getId()))
            .toList();

    Map<Long, Map<String, Long>> projectIssueStats = new LinkedHashMap<>();
    Map<Long, Integer> projectMemberCounts = new LinkedHashMap<>();
    for (Project project : ownedProjects) {
      List<Issue> issues = issueService.findByProjectId(project.getId());
      long open = issues.stream().filter(i -> i.getStatus().name().equals("OPEN")).count();
      long unassigned = issues.stream().filter(i -> i.getAssignee() == null).count();
      long inFlight =
          issues.stream()
              .filter(
                  i ->
                      i.getStatus().name().equals("ASSIGNED")
                          || i.getStatus().name().equals("IN_ANALYSIS")
                          || i.getStatus().name().equals("IN_PROGRESS"))
              .count();

      Map<String, Long> stats = new LinkedHashMap<>();
      stats.put("open", open);
      stats.put("unassigned", unassigned);
      stats.put("inProgress", inFlight);
      projectIssueStats.put(project.getId(), stats);
      projectMemberCounts.put(
          project.getId(), projectService.getProjectMembers(project.getId()).size());
    }

    model.addAttribute("currentUser", user);
    model.addAttribute("ownedProjects", ownedProjects);
    model.addAttribute("accessibleProjects", accessibleProjects);
    model.addAttribute("projectIssueStats", projectIssueStats);
    model.addAttribute("projectMemberCounts", projectMemberCounts);

    model.addAttribute("assignedIssues", assigned);
    model.addAttribute("inAnalysisIssues", inAnalysis);
    model.addAttribute("inProgressIssues", inProgress);
    model.addAttribute("resolvedIssues", resolved);

    model.addAttribute("pendingVerificationIssues", pendingVerification);
    model.addAttribute("activeVerificationIssues", myActiveVerifications);

    boolean hasDeveloperMembership =
        memberships.stream().anyMatch(member -> member.getProjectRole() == ProjectRole.DEVELOPER);
    boolean hasTesterMembership =
        memberships.stream().anyMatch(member -> member.getProjectRole() == ProjectRole.TESTER);

    model.addAttribute("hasManagedProjects", !ownedProjects.isEmpty());
    model.addAttribute("hasAssignedIssues", !myAssignedIssues.isEmpty());
    model.addAttribute(
        "hasTesterQueue", !pendingVerification.isEmpty() || !myActiveVerifications.isEmpty());
    model.addAttribute("hasDeveloperMembership", hasDeveloperMembership);
    model.addAttribute("hasTesterMembership", hasTesterMembership);

    String normalizedMode =
        switch (view == null ? "projects" : view.toLowerCase()) {
          case "developer" -> hasDeveloperMembership ? "developer" : "projects";
          case "tester" -> hasTesterMembership ? "tester" : "projects";
          default -> "projects";
        };

    model.addAttribute("activeDashboardMode", normalizedMode);
    model.addAttribute("showProjectsMode", "projects".equals(normalizedMode));
    model.addAttribute("showDeveloperMode", "developer".equals(normalizedMode));
    model.addAttribute("showTesterMode", "tester".equals(normalizedMode));

    return "dashboard/basic";
  }

  private List<Issue> filterByStatus(List<Issue> issues, String statusName) {
    return issues.stream()
        .filter(issue -> issue.getStatus().name().equals(statusName))
        .collect(Collectors.toList());
  }
}
