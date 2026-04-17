package com.jeera.service;

import com.jeera.model.Issue;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.ProjectRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

  private final IssueRepository issueRepository;
  private final UserRepository userRepository;
  private final ActivityLogService activityLogService;
  private final NotificationService notificationService;

  public Issue createIssue(Issue newIssue, Long reporterId) {
    User reporter = userRepository.findById(reporterId)
        .orElseThrow(() -> new EntityNotFoundException("Reporter not found with id: " + reporterId));

    LocalDateTime now = LocalDateTime.now();
    newIssue.setReporter(reporter);
    newIssue.setStatus(IssueStatus.REPORTED);
    newIssue.setCreatedAt(now);
    newIssue.setUpdatedAt(now);
    newIssue.setClosedAt(null);

    Issue savedIssue = issueRepository.save(newIssue);
    activityLogService.createActivityLog(savedIssue, reporter, "Issue created", null, IssueStatus.REPORTED.name());
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

  public Issue updateIssueStatus(Long issueId, IssueStatus newStatus, Long actorId) {
    Issue issue = issueRepository.findById(issueId)
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
    User actor = userRepository.findById(actorId)
        .orElseThrow(() -> new EntityNotFoundException("Actor not found with id: " + actorId));

    IssueStatus oldStatus = issue.getStatus();
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

    Issue savedIssue = issueRepository.save(issue);

    activityLogService.createActivityLog(
        savedIssue,
        actor,
        "Issue status changed",
        oldStatus.name(),
        newStatus.name());

    notifyStatusChange(savedIssue, oldStatus, newStatus);
    return savedIssue;
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
      default -> valid = false;
    }

    if (!valid) {
      throw new IllegalStateException("Invalid issue status transition: " + oldStatus + " -> " + newStatus);
    }
  }

  private boolean isTerminalStatus(IssueStatus status) {
    return status == IssueStatus.CLOSED || status == IssueStatus.REJECTED || status == IssueStatus.MARK_REJECTED;
  }

  private void notifyStatusChange(Issue issue, IssueStatus oldStatus, IssueStatus newStatus) {
    if (newStatus == IssueStatus.RESOLVED) {
      for (ProjectMember member : issue.getProject().getMembers()) {
        if (member.getProjectRole() == ProjectRole.TESTER) {
          notificationService.createNotification(
              member.getUser(),
              "Issue #" + issue.getIssueNumber() + " is ready for verification",
              issue);
        }
      }
      return;
    }

    if (newStatus == IssueStatus.CLOSED) {
      Set<Long> notifiedUserIds = new LinkedHashSet<>();
      User reporter = issue.getReporter();
      if (reporter != null && notifiedUserIds.add(reporter.getId())) {
        notificationService.createNotification(
            reporter,
            "Issue #" + issue.getIssueNumber() + " was closed",
            issue);
      }

      User assignee = issue.getAssignee();
      if (assignee != null && notifiedUserIds.add(assignee.getId())) {
        notificationService.createNotification(
            assignee,
            "Issue #" + issue.getIssueNumber() + " was closed",
            issue);
      }
      return;
    }

    if (newStatus == IssueStatus.REJECTED || newStatus == IssueStatus.MARK_REJECTED) {
      User reporter = issue.getReporter();
      if (reporter != null) {
        notificationService.createNotification(
            reporter,
            "Issue #" + issue.getIssueNumber() + " was marked " + newStatus,
            issue);
      }
      return;
    }

    if (oldStatus == IssueStatus.UNDER_VERIFICATION && newStatus == IssueStatus.OPEN) {
      User projectOwner = issue.getProject().getOwner();
      if (projectOwner != null) {
        notificationService.createNotification(
            projectOwner,
            "Issue #" + issue.getIssueNumber() + " was reopened during verification",
            issue);
      }
    }
  }
}
