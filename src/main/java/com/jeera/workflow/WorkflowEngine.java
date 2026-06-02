package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;
import java.util.Set;

/** Evaluates issue status transitions against a {@link WorkflowDefinition}. */
public interface WorkflowEngine {

  /**
   * Full validation for an actor-initiated status change: authorization rules first, then
   * transition-table validity. Throws {@link IllegalStateException} (with a user-facing message)
   * when the transition is not permitted.
   */
  void validateTransition(TransitionContext context);

  /**
   * Edge-only validity check (no actor authorization). Used where the caller performs its own
   * authorization, e.g. developer assignment. Throws {@link IllegalStateException} on an invalid
   * edge.
   */
  void requireValidPath(IssueStatus from, IssueStatus to);

  /**
   * @return the statuses reachable from {@code from} (excludes the self edge).
   */
  Set<IssueStatus> allowedNextStatuses(IssueStatus from);
}
