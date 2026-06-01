package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;
import java.util.function.BiPredicate;

/**
 * Authorization rule that is satisfied only by a PM (project owner) or a system
 * admin, i.e. {@link TransitionContext#canManageIssue()}.
 */
public final class ManageAuthorizationRule implements AuthorizationRule {

  private final BiPredicate<IssueStatus, IssueStatus> applicability;
  private final String message;

  public ManageAuthorizationRule(BiPredicate<IssueStatus, IssueStatus> applicability, String message) {
    this.applicability = applicability;
    this.message = message;
  }

  @Override
  public boolean appliesTo(IssueStatus from, IssueStatus to) {
    return applicability.test(from, to);
  }

  @Override
  public void enforce(TransitionContext context) {
    if (!context.canManageIssue()) {
      throw new IllegalStateException(message);
    }
  }
}
