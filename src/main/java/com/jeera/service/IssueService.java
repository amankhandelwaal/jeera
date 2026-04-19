package com.jeera.service;

import com.jeera.model.Issue;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

  private final IssueRepository issueRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ActivityLogService activityLogService;
  private final NotificationService notificationService;

  public Issue findById(Long issueId) {
    return issueRepository.findPageDetailById(issueId)
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
  }

  public List<Issue> findByProjectId(Long projectId) {
    return issueRepository.findPageListByProjectId(projectId);
  }

  public Issue createIssue(Issue newIssue, Long reporterId) {
    User reporter = userRepository.findById(reporterId)
        .orElseThrow(() -> new EntityNotFoundException("Reporter not found with id: " + reporterId));

    if (newIssue.getProject() == null || newIssue.getProject().getId() == null) {
      throw new IllegalStateException("Project is required to create an issue");
    }

    Integer currentMaxIssueNumber = issueRepository.findMaxIssueNumberByProjectId(newIssue.getProject().getId());
    newIssue.setIssueNumber((currentMaxIssueNumber == null ? 0 : currentMaxIssueNumber) + 1);

    LocalDateTime now = LocalDateTime.now();
    newIssue.setReporter(reporter);
    newIssue.setStatus(IssueStatus.REPORTED);
    newIssue.setCreatedAt(now);
    newIssue.setUpdatedAt(now);
    newIssue.setClosedAt(null);

    Issue savedIssue = issueRepository.save(newIssue);
    activityLogService.createActivityLog(savedIssue, reporter, "Issue created", null, IssueStatus.REPORTED.name());

    User projectOwner = savedIssue.getProject() != null ? savedIssue.getProject().getOwner() : null;
    if (projectOwner != null && !projectOwner.getId().equals(reporter.getId())) {
      notificationService.createNotification(
          projectOwner,
          "New issue #" + savedIssue.getIssueNumber() + " was created in your project",
          savedIssue);
    }

    return savedIssue;
  }

  public Issue assignDeveloper(Long issueId, Long assigneeId, Long actorId) {
    Issue issue = issueRepository.findById(issueId)
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
    User assignee = userRepository.findById(assigneeId)
        .orElseThrow(() -> new EntityNotFoundException("Assignee not found with id: " + assigneeId));
    User actor = userRepository.findById(actorId)
        .orElseThrow(() -> new EntityNotFoundException("Actor not found with id: " + actorId));

    IssueStatus oldStatus = issue.getStatus();
    validateTransition(oldStatus, IssueStatus.ASSIGNED);

    ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(issue.getProject().getId(), assigneeId)
        .orElseThrow(() -> new IllegalStateException("Assignee must be a member of this project"));
    if (membership.getProjectRole() != ProjectRole.DEVELOPER) {
      throw new IllegalStateException("Assignee must have DEVELOPER role in this project");
    }
    if (assignee.getSystemRole() == UserRole.ADMIN) {
      throw new IllegalStateException("Admin users cannot be assigned to project issues");
    }

    issue.setAssignee(assignee);
    issue.setStatus(IssueStatus.ASSIGNED);
    issue.setUpdatedAt(LocalDateTime.now());
    issue.setClosedAt(null);

    Issue savedIssue = issueRepository.save(issue);

    activityLogService.createActivityLog(
        savedIssue,
        actor,
        "Issue assigned to developer",
        oldStatus.name(),
        IssueStatus.ASSIGNED.name());

    notificationService.createNotification(
        assignee,
        "You were assigned issue #" + savedIssue.getIssueNumber(),
        savedIssue);

    return savedIssue;
  }

  public List<User> getAssignableDevelopers(Long projectId) {
    return projectMemberRepository.findByProjectIdAndProjectRole(projectId, ProjectRole.DEVELOPER)
        .stream()
        .map(ProjectMember::getUser)
        .filter(user -> user.getSystemRole() != UserRole.ADMIN)
        .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public Issue updateIssueStatus(Long issueId, IssueStatus newStatus, Long actorId) {
    Issue issue = issueRepository.findById(issueId)
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
    User actor = userRepository.findById(actorId)
        .orElseThrow(() -> new EntityNotFoundException("Actor not found with id: " + actorId));

    IssueStatus oldStatus = issue.getStatus();
    validateActorCanPerformTransition(issue, oldStatus, newStatus, actor);
    validateTransition(oldStatus, newStatus);

    issue.setStatus(newStatus);
    issue.setUpdatedAt(LocalDateTime.now());

    if (isTerminalStatus(newStatus)) {
      issue.setClosedAt(LocalDateTime.now());
    } else {
      issue.setClosedAt(null);
    }

    if (oldStatus == IssueStatus.UNDER_VERIFICATION && newStatus == IssueStatus.OPEN) {
      issue.setAssignee(null);
    }

    if (newStatus == IssueStatus.UNDER_VERIFICATION) {
      ProjectMember testerMembership = projectMemberRepository
          .findByProjectIdAndUserId(issue.getProject().getId(), actor.getId())
          .orElseThrow(() -> new IllegalStateException("Only project testers can pick issues for verification"));
      if (testerMembership.getProjectRole() != ProjectRole.TESTER) {
        throw new IllegalStateException("Only project testers can pick issues for verification");
      }
      issue.setAssignee(actor);
    }

    Issue savedIssue = issueRepository.save(issue);

    activityLogService.createActivityLog(
        savedIssue,
        actor,
        "Issue status changed",
        oldStatus.name(),
        newStatus.name());

    notifyStatusChange(savedIssue, oldStatus, newStatus, actor);
    return savedIssue;
  }

  public Issue pickUpForVerification(Long issueId, Long actorId) {
    return updateIssueStatus(issueId, IssueStatus.UNDER_VERIFICATION, actorId);
  }

  private void validateTransition(IssueStatus oldStatus, IssueStatus newStatus) {
    if (oldStatus == newStatus) {
      return;
    }

    boolean valid;
    switch (oldStatus) {
      case REPORTED -> valid = (newStatus == IssueStatus.OPEN || newStatus == IssueStatus.REJECTED);
      case OPEN -> valid = (newStatus == IssueStatus.ASSIGNED || newStatus == IssueStatus.REJECTED);
      case ASSIGNED -> valid = (newStatus == IssueStatus.IN_ANALYSIS);
      case IN_ANALYSIS -> valid = (newStatus == IssueStatus.IN_PROGRESS || newStatus == IssueStatus.MARK_REJECTED);
      case IN_PROGRESS -> valid = (newStatus == IssueStatus.RESOLVED);
      case RESOLVED -> valid = (newStatus == IssueStatus.UNDER_VERIFICATION);
      case UNDER_VERIFICATION -> valid = (newStatus == IssueStatus.CLOSED || newStatus == IssueStatus.OPEN);
      case MARK_REJECTED -> valid = (newStatus == IssueStatus.OPEN || newStatus == IssueStatus.REJECTED);
      case REJECTED -> valid = false;
      default -> valid = false;
    }

    if (!valid) {
      throw new IllegalStateException("Invalid issue status transition: " + oldStatus + " -> " + newStatus);
    }
  }

  private boolean isTerminalStatus(IssueStatus status) {
    return status == IssueStatus.CLOSED || status == IssueStatus.REJECTED || status == IssueStatus.MARK_REJECTED;
  }

  private void notifyStatusChange(Issue issue, IssueStatus oldStatus, IssueStatus newStatus, User actor) {
    Set<Long> notifiedUserIds = new LinkedHashSet<>();
    notifyProjectOwnerForExternalAction(issue, oldStatus, newStatus, actor, notifiedUserIds);

    if (newStatus == IssueStatus.RESOLVED) {
      for (ProjectMember member : issue.getProject().getMembers()) {
        if (member.getProjectRole() == ProjectRole.TESTER) {
          sendIssueNotification(
              member.getUser(),
              "Issue #" + issue.getIssueNumber() + " is ready for verification",
              issue,
              notifiedUserIds);
        }
      }
      return;
    }

    if (newStatus == IssueStatus.CLOSED) {
      User reporter = issue.getReporter();
      sendIssueNotification(
          reporter,
          "Issue #" + issue.getIssueNumber() + " was closed",
          issue,
          notifiedUserIds);

      User assignee = issue.getAssignee();
      sendIssueNotification(
          assignee,
          "Issue #" + issue.getIssueNumber() + " was closed",
          issue,
          notifiedUserIds);
      return;
    }

    if (newStatus == IssueStatus.REJECTED || newStatus == IssueStatus.MARK_REJECTED) {
      sendIssueNotification(
          issue.getReporter(),
          "Issue #" + issue.getIssueNumber() + " was marked " + newStatus,
          issue,
          notifiedUserIds);
    }
  }

  private void notifyProjectOwnerForExternalAction(
      Issue issue,
      IssueStatus oldStatus,
      IssueStatus newStatus,
      User actor,
      Set<Long> notifiedUserIds) {
    User projectOwner = issue.getProject() != null ? issue.getProject().getOwner() : null;
    if (projectOwner == null || actor == null || projectOwner.getId().equals(actor.getId())) {
      return;
    }

    sendIssueNotification(
        projectOwner,
        "Issue #" + issue.getIssueNumber() + " moved from " + oldStatus + " to " + newStatus,
        issue,
        notifiedUserIds);
  }

  private void sendIssueNotification(User recipient, String message, Issue issue, Set<Long> notifiedUserIds) {
    if (recipient == null || recipient.getId() == null) {
      return;
    }
    if (notifiedUserIds.add(recipient.getId())) {
      notificationService.createNotification(recipient, message, issue);
    }
  }

  private void validateActorCanPerformTransition(Issue issue, IssueStatus oldStatus, IssueStatus newStatus,
      User actor) {
    boolean isProjectOwner = issue.getProject() != null
        && issue.getProject().getOwner() != null
        && issue.getProject().getOwner().getId().equals(actor.getId());
    boolean isAdmin = actor.getSystemRole() == UserRole.ADMIN;
    boolean canManageIssue = isProjectOwner || isAdmin;

    if (newStatus == IssueStatus.REJECTED && !canManageIssue) {
      throw new IllegalStateException("Only PM/Admin can mark an issue as REJECTED");
    }

    if (oldStatus == IssueStatus.MARK_REJECTED && !canManageIssue) {
      throw new IllegalStateException("Only PM/Admin can review MARK_REJECTED issues");
    }
  }
}
