package com.jeera.event;

import com.jeera.model.Issue;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.ProjectRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.UserRepository;
import com.jeera.service.NotificationService;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates issue domain events into user notifications. This decouples {@link
 * com.jeera.service.IssueService} from {@link NotificationService}: the service publishes events,
 * this listener decides who to notify.
 *
 * <p>Listeners run synchronously within the originating transaction, preserving the previous
 * behaviour where notifications were written alongside the issue change. (A later milestone may
 * move these to {@code AFTER_COMMIT} once the test suite can cover the changed failure semantics.)
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;
  private final IssueRepository issueRepository;
  private final UserRepository userRepository;

  @EventListener
  public void onIssueCreated(IssueCreatedEvent event) {
    Issue issue = issueRepository.findById(event.issueId()).orElse(null);
    if (issue == null) {
      return;
    }
    User reporter = issue.getReporter();
    User projectOwner = issue.getProject() != null ? issue.getProject().getOwner() : null;
    if (projectOwner != null
        && reporter != null
        && !projectOwner.getId().equals(reporter.getId())) {
      notificationService.createNotification(
          projectOwner,
          "New issue #" + issue.getIssueNumber() + " was created in your project",
          issue);
    }
  }

  @EventListener
  public void onIssueAssigned(IssueAssignedEvent event) {
    Issue issue = issueRepository.findById(event.issueId()).orElse(null);
    if (issue == null) {
      return;
    }
    User assignee = userRepository.findById(event.assigneeId()).orElse(null);
    if (assignee == null) {
      return;
    }
    notificationService.createNotification(
        assignee, "You were assigned issue #" + issue.getIssueNumber(), issue);
  }

  @EventListener
  public void onIssueTransitioned(IssueTransitionedEvent event) {
    Issue issue = issueRepository.findById(event.issueId()).orElse(null);
    if (issue == null) {
      return;
    }
    User actor = userRepository.findById(event.actorId()).orElse(null);
    notifyStatusChange(issue, event.oldStatus(), event.newStatus(), actor);
  }

  // --- moved verbatim from the former IssueService.notify* methods -----------

  private void notifyStatusChange(
      Issue issue, IssueStatus oldStatus, IssueStatus newStatus, User actor) {
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
      sendIssueNotification(
          issue.getReporter(),
          "Issue #" + issue.getIssueNumber() + " was closed",
          issue,
          notifiedUserIds);
      sendIssueNotification(
          issue.getAssignee(),
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

  private void sendIssueNotification(
      User recipient, String message, Issue issue, Set<Long> notifiedUserIds) {
    if (recipient == null || recipient.getId() == null) {
      return;
    }
    if (notifiedUserIds.add(recipient.getId())) {
      notificationService.createNotification(recipient, message, issue);
    }
  }
}
