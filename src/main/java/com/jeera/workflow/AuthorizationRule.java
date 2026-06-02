package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;

/**
 * A pre-edge authorization check. Rules are evaluated, in order, before the transition table is
 * consulted — mirroring the original {@code validateActorCanPerformTransition} behaviour where
 * actor checks ran ahead of edge-validity checks.
 */
public interface AuthorizationRule {

  /**
   * @return true when this rule governs the given status edge.
   */
  boolean appliesTo(IssueStatus from, IssueStatus to);

  /** Throws {@link IllegalStateException} when the acting user is not permitted. */
  void enforce(TransitionContext context);
}
