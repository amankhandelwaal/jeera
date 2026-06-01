package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven description of the issue workflow: the allowed status edges plus
 * the ordered authorization rules guarding them. Replaces the hand-coded
 * {@code switch} that previously lived in {@code IssueService}.
 */
public class WorkflowDefinition {

  private final Map<IssueStatus, Set<IssueStatus>> transitions;
  private final List<AuthorizationRule> authorizationRules;

  public WorkflowDefinition(Map<IssueStatus, Set<IssueStatus>> transitions,
      List<AuthorizationRule> authorizationRules) {
    this.transitions = new EnumMap<>(transitions);
    this.authorizationRules = List.copyOf(authorizationRules);
  }

  /** @return the statuses directly reachable from {@code from} (never null). */
  public Set<IssueStatus> allowedTransitions(IssueStatus from) {
    return transitions.getOrDefault(from, Collections.emptySet());
  }

  /** @return the authorization rules, evaluated in order before edge validity. */
  public List<AuthorizationRule> authorizationRules() {
    return authorizationRules;
  }
}
