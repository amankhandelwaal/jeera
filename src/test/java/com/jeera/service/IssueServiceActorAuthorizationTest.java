package com.jeera.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jeera.event.IssueTransitionedEvent;
import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.ProjectMember;
import com.jeera.model.User;
import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.IssueType;
import com.jeera.model.enums.ProjectRole;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.ProjectMemberRepository;
import com.jeera.repository.UserRepository;
import com.jeera.workflow.DefaultWorkflowEngine;
import com.jeera.workflow.DefaultWorkflowProvider;
import com.jeera.workflow.WorkflowEngine;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Exercises {@link IssueService}'s authorization and side-effect behaviour using
 * mocked repositories but the <em>real</em> {@link DefaultWorkflowEngine}, so the
 * service-plus-engine wiring is tested together.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IssueServiceActorAuthorizationTest {

  @Mock private IssueRepository issueRepository;
  @Mock private UserRepository userRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ActivityLogService activityLogService;
  @Mock private ApplicationEventPublisher eventPublisher;

  private final WorkflowEngine workflowEngine = new DefaultWorkflowEngine(new DefaultWorkflowProvider());
  private IssueService issueService;

  private User owner;
  private User developer;
  private User tester;
  private User outsider;
  private Project project;

  @BeforeEach
  void setUp() {
    issueService = new IssueService(issueRepository, userRepository, projectMemberRepository,
        activityLogService, workflowEngine, eventPublisher);

    owner = User.builder().id(1L).username("owner").systemRole(UserRole.USER).build();
    developer = User.builder().id(2L).username("dev").systemRole(UserRole.USER).build();
    tester = User.builder().id(3L).username("tester").systemRole(UserRole.USER).build();
    outsider = User.builder().id(4L).username("outsider").systemRole(UserRole.USER).build();
    project = Project.builder().id(10L).name("proj").owner(owner).build();

    lenient().when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
    lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(developer));
    lenient().when(userRepository.findById(3L)).thenReturn(Optional.of(tester));
    lenient().when(userRepository.findById(4L)).thenReturn(Optional.of(outsider));
  }

  private Issue issue(IssueStatus status, User assignee) {
    return Issue.builder()
        .id(100L).issueNumber(1).project(project).title("t")
        .type(IssueType.BUG).priority(IssuePriority.MEDIUM).status(status).assignee(assignee)
        .build();
  }

  private ProjectMember membership(ProjectRole role, User user) {
    return ProjectMember.builder().projectRole(role).user(user).build();
  }

  // --- updateIssueStatus authorization ---

  @Test
  void rejectingRequiresPmOrAdmin() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.updateIssueStatus(100L, IssueStatus.REJECTED, outsider.getId()));
    assertEquals("Only PM/Admin can mark an issue as REJECTED", ex.getMessage());
    verify(issueRepository, never()).save(any());
  }

  @Test
  void projectOwnerCanReject() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    Issue result = issueService.updateIssueStatus(100L, IssueStatus.REJECTED, owner.getId());
    assertEquals(IssueStatus.REJECTED, result.getStatus());
    assertNotNull(result.getClosedAt());
    verify(eventPublisher).publishEvent(any(IssueTransitionedEvent.class));
  }

  @Test
  void adminCanReject() {
    User admin = User.builder().id(9L).username("admin").systemRole(UserRole.ADMIN).build();
    lenient().when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    Issue result = issueService.updateIssueStatus(100L, IssueStatus.REJECTED, admin.getId());
    assertEquals(IssueStatus.REJECTED, result.getStatus());
  }

  @Test
  void pickingForVerificationRequiresTesterRole() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.RESOLVED, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, developer.getId()))
        .thenReturn(Optional.of(membership(ProjectRole.DEVELOPER, developer)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.updateIssueStatus(100L, IssueStatus.UNDER_VERIFICATION, developer.getId()));
    assertEquals("Only project testers can pick issues for verification", ex.getMessage());
  }

  @Test
  void testerPickupAssignsIssueToTester() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.RESOLVED, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, tester.getId()))
        .thenReturn(Optional.of(membership(ProjectRole.TESTER, tester)));
    Issue result = issueService.updateIssueStatus(100L, IssueStatus.UNDER_VERIFICATION, tester.getId());
    assertEquals(IssueStatus.UNDER_VERIFICATION, result.getStatus());
    assertSame(tester, result.getAssignee());
  }

  @Test
  void reopenFromVerificationClearsAssignee() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.UNDER_VERIFICATION, tester)));
    Issue result = issueService.updateIssueStatus(100L, IssueStatus.OPEN, owner.getId());
    assertEquals(IssueStatus.OPEN, result.getStatus());
    assertNull(result.getAssignee());
  }

  @Test
  void invalidTransitionIsRejected() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.updateIssueStatus(100L, IssueStatus.RESOLVED, owner.getId()));
    assertEquals("Invalid issue status transition: OPEN -> RESOLVED", ex.getMessage());
  }

  // --- assignDeveloper ---

  @Test
  void assignDeveloper_rejectsNonMember() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, developer.getId())).thenReturn(Optional.empty());
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.assignDeveloper(100L, developer.getId(), owner.getId()));
    assertEquals("Assignee must be a member of this project", ex.getMessage());
  }

  @Test
  void assignDeveloper_rejectsNonDeveloperRole() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, tester.getId()))
        .thenReturn(Optional.of(membership(ProjectRole.TESTER, tester)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.assignDeveloper(100L, tester.getId(), owner.getId()));
    assertEquals("Assignee must have DEVELOPER role in this project", ex.getMessage());
  }

  @Test
  void assignDeveloper_rejectsAdminAssignee() {
    User admin = User.builder().id(9L).username("admin").systemRole(UserRole.ADMIN).build();
    lenient().when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, admin.getId()))
        .thenReturn(Optional.of(membership(ProjectRole.DEVELOPER, admin)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.assignDeveloper(100L, admin.getId(), owner.getId()));
    assertEquals("Admin users cannot be assigned to project issues", ex.getMessage());
  }

  @Test
  void assignDeveloper_succeedsForValidDeveloper() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.OPEN, null)));
    when(projectMemberRepository.findByProjectIdAndUserId(10L, developer.getId()))
        .thenReturn(Optional.of(membership(ProjectRole.DEVELOPER, developer)));
    Issue result = issueService.assignDeveloper(100L, developer.getId(), owner.getId());
    assertEquals(IssueStatus.ASSIGNED, result.getStatus());
    assertSame(developer, result.getAssignee());
  }

  @Test
  void assignDeveloper_rejectsInvalidSourceState() {
    when(issueRepository.findById(100L)).thenReturn(Optional.of(issue(IssueStatus.REPORTED, null)));
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> issueService.assignDeveloper(100L, developer.getId(), owner.getId()));
    assertEquals("Invalid issue status transition: REPORTED -> ASSIGNED", ex.getMessage());
  }
}
