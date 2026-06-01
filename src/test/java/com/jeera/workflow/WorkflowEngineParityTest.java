package com.jeera.workflow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jeera.model.enums.IssueStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the data-driven {@link DefaultWorkflowEngine} reproduces — for every
 * {@code (from, to, canManage)} combination, including the exact error messages
 * — the behaviour of the original hand-coded state machine that used to live in
 * {@code IssueService}. The legacy logic is duplicated here as the oracle; this
 * test must pass before the legacy methods are deleted.
 */
class WorkflowEngineParityTest {

  private final WorkflowEngine engine = new DefaultWorkflowEngine(new DefaultWorkflowProvider());

  @Test
  @DisplayName("engine reproduces the legacy state machine for every status pair")
  void engineMatchesLegacyStateMachine() {
    List<String> mismatches = new ArrayList<>();

    for (IssueStatus from : IssueStatus.values()) {
      for (IssueStatus to : IssueStatus.values()) {
        for (boolean canManage : new boolean[] {true, false}) {
          Optional<String> expected = legacyValidate(from, to, canManage);
          Optional<String> actual = captureError(
              () -> engine.validateTransition(new TransitionContext(from, to, canManage)));
          if (!expected.equals(actual)) {
            mismatches.add("validateTransition " + from + "->" + to + " canManage=" + canManage
                + " expected=" + expected + " actual=" + actual);
          }
        }

        Optional<String> expectedPath = legacyPath(from, to);
        Optional<String> actualPath = captureError(() -> engine.requireValidPath(from, to));
        if (!expectedPath.equals(actualPath)) {
          mismatches.add("requireValidPath " + from + "->" + to
              + " expected=" + expectedPath + " actual=" + actualPath);
        }
      }
    }

    assertTrue(mismatches.isEmpty(),
        "Engine diverged from legacy state machine:\n" + String.join("\n", mismatches));
  }

  private static Optional<String> captureError(Runnable runnable) {
    try {
      runnable.run();
      return Optional.empty();
    } catch (IllegalStateException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  // --- legacy oracle: copied verbatim from the original IssueService methods ---

  private static Optional<String> legacyValidate(IssueStatus oldStatus, IssueStatus newStatus, boolean canManage) {
    if (newStatus == IssueStatus.REJECTED && !canManage) {
      return Optional.of("Only PM/Admin can mark an issue as REJECTED");
    }
    if (oldStatus == IssueStatus.MARK_REJECTED && !canManage) {
      return Optional.of("Only PM/Admin can review MARK_REJECTED issues");
    }
    return legacyPath(oldStatus, newStatus);
  }

  private static Optional<String> legacyPath(IssueStatus oldStatus, IssueStatus newStatus) {
    if (oldStatus == newStatus) {
      return Optional.empty();
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
      return Optional.of("Invalid issue status transition: " + oldStatus + " -> " + newStatus);
    }
    return Optional.empty();
  }
}
