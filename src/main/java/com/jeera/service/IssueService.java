package com.jeera.service;

import com.jeera.event.IssueAssignedEvent;
import com.jeera.event.IssueCreatedEvent;
import com.jeera.event.IssueTransitionedEvent;
import com.jeera.model.Issue;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.UserRepository;
import com.jeera.workflow.TransitionContext;
import com.jeera.workflow.WorkflowEngine;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

  private final IssueRepository issueRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ActivityLogService activityLogService;
  private final WorkflowEngine workflowEngine;
  private final ApplicationEventPublisher eventPublisher;

  public Issue findById(Long issueId) {
    return issueRepository
        .findPageDetailById(issueId)
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
  }

  public List<Issue> findByProjectId(Long projectId) {
    return issueRepository.findPageListByProjectId(projectId);
  }

  public Issue createIssue(Issue newIssue, Long reporterId) {
    User reporter =
        userRepository
            .findById(reporterId)
            .orElseThrow(
                () -> new EntityNotFoundException("Reporter not found with id: " + reporterId));

    if (newIssue.getProject() == null || newIssue.getProject().getId() == null) {
      throw new IllegalStateException("Project is required to create an issue");
    }

    Integer currentMaxIssueNumber =
        issueRepository.findMaxIssueNumberByProjectId(newIssue.getProject().getId());
    newIssue.setIssueNumber((currentMaxIssueNumber == null ? 0 : currentMaxIssueNumber) + 1);

    LocalDateTime now = LocalDateTime.now();
    newIssue.setReporter(reporter);
    newIssue.setStatus(IssueStatus.REPORTED);
    newIssue.setCreatedAt(now);
    newIssue.setUpdatedAt(now);
    newIssue.setClosedAt(null);

    Issue savedIssue = issueRepository.save(newIssue);
    activityLogService.createActivityLog(
        savedIssue, reporter, "Issue created", null, IssueStatus.REPORTED.name());

    eventPublisher.publishEvent(new IssueCreatedEvent(savedIssue.getId()));

    return savedIssue;
  }

  public Issue assignDeveloper(Long issueId, Long assigneeId, Long actorId) {
    Issue issue =
        issueRepository
            .findById(issueId)
            .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
    User assignee =
        userRepository
            .findById(assigneeId)
            .orElseThrow(
                () -> new EntityNotFoundException("Assignee not found with id: " + assigneeId));
    User actor =
        userRepository
            .findById(actorId)
            .orElseThrow(() -> new EntityNotFoundException("Actor not found with id: " + actorId));

    IssueStatus oldStatus = issue.getStatus();
    // Edge-only validation: assignment performs its own membership/role checks below
    // (the legacy code did not apply the PM/Admin manage gate here).
    workflowEngine.requireValidPath(oldStatus, IssueStatus.ASSIGNED);

    ProjectMember membership =
        projectMemberRepository
            .findByProjectIdAndUserId(issue.getProject().getId(), assigneeId)
            .orElseThrow(
                () -> new IllegalStateException("Assignee must be a member of this project"));
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

    eventPublisher.publishEvent(
        new IssueAssignedEvent(savedIssue.getId(), assignee.getId(), actor.getId()));

    return savedIssue;
  }

  public List<User> getAssignableDevelopers(Long projectId) {
    return projectMemberRepository
        .findByProjectIdAndProjectRole(projectId, ProjectRole.DEVELOPER)
        .stream()
        .map(ProjectMember::getUser)
        .filter(user -> user.getSystemRole() != UserRole.ADMIN)
        .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public Issue updateIssueStatus(Long issueId, IssueStatus newStatus, Long actorId) {
    Issue issue =
        issueRepository
            .findById(issueId)
            .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));
    User actor =
        userRepository
            .findById(actorId)
            .orElseThrow(() -> new EntityNotFoundException("Actor not found with id: " + actorId));

    IssueStatus oldStatus = issue.getStatus();
    boolean canManageIssue = canManageIssue(issue, actor);
    workflowEngine.validateTransition(new TransitionContext(oldStatus, newStatus, canManageIssue));

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
      ProjectMember testerMembership =
          projectMemberRepository
              .findByProjectIdAndUserId(issue.getProject().getId(), actor.getId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Only project testers can pick issues for verification"));
      if (testerMembership.getProjectRole() != ProjectRole.TESTER) {
        throw new IllegalStateException("Only project testers can pick issues for verification");
      }
      issue.setAssignee(actor);
    }

    Issue savedIssue = issueRepository.save(issue);

    activityLogService.createActivityLog(
        savedIssue, actor, "Issue status changed", oldStatus.name(), newStatus.name());

    eventPublisher.publishEvent(
        new IssueTransitionedEvent(savedIssue.getId(), oldStatus, newStatus, actor.getId()));
    return savedIssue;
  }

  public Issue pickUpForVerification(Long issueId, Long actorId) {
    return updateIssueStatus(issueId, IssueStatus.UNDER_VERIFICATION, actorId);
  }

  private boolean canManageIssue(Issue issue, User actor) {
    boolean isProjectOwner =
        issue.getProject() != null
            && issue.getProject().getOwner() != null
            && issue.getProject().getOwner().getId().equals(actor.getId());
    boolean isAdmin = actor.getSystemRole() == UserRole.ADMIN;
    return isProjectOwner || isAdmin;
  }

  private boolean isTerminalStatus(IssueStatus status) {
    return status == IssueStatus.CLOSED
        || status == IssueStatus.REJECTED
        || status == IssueStatus.MARK_REJECTED;
  }
}
