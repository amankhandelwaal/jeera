package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;

/**
 * Immutable inputs needed to evaluate a single workflow transition.
 *
 * @param from the issue's current status
 * @param to the requested next status
 * @param canManageIssue whether the acting user is the project owner (PM) or a system admin
 */
public record TransitionContext(IssueStatus from, IssueStatus to, boolean canManageIssue) {}
