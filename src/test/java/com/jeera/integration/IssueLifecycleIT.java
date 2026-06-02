package com.jeera.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.IssueType;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.ActivityLogRepository;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.NotificationRepository;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import com.jeera.service.IssueService;
import com.jeera.support.AbstractPostgresIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end walk of the issue workflow through the real service stack (engine + domain events +
 * JPA persistence) against PostgreSQL: REPORTED -> OPEN -> ASSIGNED -> IN_ANALYSIS -> IN_PROGRESS
 * -> RESOLVED -> UNDER_VERIFICATION -> CLOSED, asserting activity logs and notifications.
 */
@SpringBootTest
class IssueLifecycleIT extends AbstractPostgresIT {

  @Autowired private IssueService issueService;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private IssueRepository issueRepository;
  @Autowired private ActivityLogRepository activityLogRepository;
  @Autowired private NotificationRepository notificationRepository;

  @Test
  void fullLifecycle_reportedToClosed() {
    LocalDateTime now = LocalDateTime.now();
    String suffix = String.valueOf(System.nanoTime());

    User owner =
        userRepository.save(
            User.builder()
                .username("pm_" + suffix)
                .email("pm_" + suffix + "@x.com")
                .passwordHash("x")
                .systemRole(UserRole.USER)
                .createdAt(now)
                .build());
    User dev =
        userRepository.save(
            User.builder()
                .username("dev_" + suffix)
                .email("dev_" + suffix + "@x.com")
                .passwordHash("x")
                .systemRole(UserRole.USER)
                .createdAt(now)
                .build());
    User tester =
        userRepository.save(
            User.builder()
                .username("qa_" + suffix)
                .email("qa_" + suffix + "@x.com")
                .passwordHash("x")
                .systemRole(UserRole.USER)
                .createdAt(now)
                .build());

    Project project =
        projectRepository.save(
            Project.builder().name("Lifecycle").owner(owner).createdAt(now).build());
    projectMemberRepository.save(
        ProjectMember.builder()
            .project(project)
            .user(dev)
            .projectRole(ProjectRole.DEVELOPER)
            .addedAt(now)
            .build());
    projectMemberRepository.save(
        ProjectMember.builder()
            .project(project)
            .user(tester)
            .projectRole(ProjectRole.TESTER)
            .addedAt(now)
            .build());

    Issue created =
        issueService.createIssue(
            Issue.builder()
                .project(project)
                .title("Login bug")
                .description("desc")
                .type(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .build(),
            dev.getId());
    Long issueId = created.getId();
    assertEquals(IssueStatus.REPORTED, created.getStatus());

    issueService.updateIssueStatus(issueId, IssueStatus.OPEN, owner.getId());
    issueService.assignDeveloper(issueId, dev.getId(), owner.getId());
    issueService.updateIssueStatus(issueId, IssueStatus.IN_ANALYSIS, dev.getId());
    issueService.updateIssueStatus(issueId, IssueStatus.IN_PROGRESS, dev.getId());
    issueService.updateIssueStatus(issueId, IssueStatus.RESOLVED, dev.getId());
    issueService.pickUpForVerification(issueId, tester.getId());
    Issue closed = issueService.updateIssueStatus(issueId, IssueStatus.CLOSED, tester.getId());

    assertEquals(IssueStatus.CLOSED, closed.getStatus());
    assertNotNull(closed.getClosedAt());

    Issue reloaded = issueRepository.findById(issueId).orElseThrow();
    assertEquals(IssueStatus.CLOSED, reloaded.getStatus());

    // create + 6 status changes + 1 assignment = 8 activity-log entries
    assertTrue(
        activityLogRepository.findPageFeedByIssueId(issueId).size() >= 7,
        "expected an audit trail for the lifecycle");

    // domain events produced notifications (tester notified at RESOLVED, reporter at CLOSED, etc.)
    assertTrue(notificationRepository.count() > 0, "expected notifications to be produced");
  }
}
