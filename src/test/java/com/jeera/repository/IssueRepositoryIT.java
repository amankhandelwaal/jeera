package com.jeera.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jeera.model.Issue;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.IssueType;
import com.jeera.model.enums.UserRole;
import com.jeera.support.AbstractPostgresIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies the custom IssueRepository queries and the project-scoped issue-number unique constraint
 * against a real PostgreSQL instance. Each test seeds its own project/user (unique names) so
 * assertions are isolated without transactional rollback.
 */
@SpringBootTest
class IssueRepositoryIT extends AbstractPostgresIT {

  @Autowired private IssueRepository issueRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;

  private Project project;
  private User reporter;

  @BeforeEach
  void seed() {
    String suffix = String.valueOf(System.nanoTime());
    reporter =
        userRepository.save(
            User.builder()
                .username("owner_" + suffix)
                .email("owner_" + suffix + "@example.com")
                .passwordHash("x")
                .systemRole(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build());
    project =
        projectRepository.save(
            Project.builder().name("Proj").owner(reporter).createdAt(LocalDateTime.now()).build());
  }

  private Issue saveIssue(int number) {
    return issueRepository.save(
        Issue.builder()
            .project(project)
            .issueNumber(number)
            .title("t" + number)
            .type(IssueType.BUG)
            .priority(IssuePriority.MEDIUM)
            .status(IssueStatus.REPORTED)
            .reporter(reporter)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  @Test
  void findMaxIssueNumber_returnsZeroWhenNoIssues() {
    assertEquals(0, issueRepository.findMaxIssueNumberByProjectId(project.getId()).intValue());
  }

  @Test
  void findMaxIssueNumber_returnsHighest() {
    saveIssue(1);
    saveIssue(2);
    saveIssue(3);
    assertEquals(3, issueRepository.findMaxIssueNumberByProjectId(project.getId()).intValue());
  }

  @Test
  void findByProjectIdAndIssueNumber_resolvesIssue() {
    Issue saved = saveIssue(7);
    assertTrue(issueRepository.findByProjectIdAndIssueNumber(project.getId(), 7).isPresent());
    assertEquals(
        saved.getId(),
        issueRepository.findByProjectIdAndIssueNumber(project.getId(), 7).orElseThrow().getId());
  }

  @Test
  void uniqueConstraint_rejectsDuplicateIssueNumber() {
    saveIssue(1);
    // IDENTITY id generation forces an immediate INSERT, so the duplicate
    // (project_id, issue_number) violation surfaces on save.
    assertThrows(DataIntegrityViolationException.class, () -> saveIssue(1));
  }
}
