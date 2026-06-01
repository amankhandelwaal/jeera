package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The built-in Jeera workflow. This mirrors, exactly, the hand-coded state
 * machine that previously lived in {@code IssueService}, so behaviour is
 * preserved (see {@code WorkflowEngineParityTest}).
 */
@Component
public class DefaultWorkflowProvider implements WorkflowProvider {

  @Override
  public WorkflowDefinition getWorkflow() {
    Map<IssueStatus, Set<IssueStatus>> transitions = new EnumMap<>(IssueStatus.class);
    transitions.put(IssueStatus.REPORTED, EnumSet.of(IssueStatus.OPEN, IssueStatus.REJECTED));
    transitions.put(IssueStatus.OPEN, EnumSet.of(IssueStatus.ASSIGNED, IssueStatus.REJECTED));
    transitions.put(IssueStatus.ASSIGNED, EnumSet.of(IssueStatus.IN_ANALYSIS));
    transitions.put(IssueStatus.IN_ANALYSIS, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.MARK_REJECTED));
    transitions.put(IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.RESOLVED));
    transitions.put(IssueStatus.RESOLVED, EnumSet.of(IssueStatus.UNDER_VERIFICATION));
    transitions.put(IssueStatus.UNDER_VERIFICATION, EnumSet.of(IssueStatus.CLOSED, IssueStatus.OPEN));
    transitions.put(IssueStatus.MARK_REJECTED, EnumSet.of(IssueStatus.OPEN, IssueStatus.REJECTED));
    // Terminal states: no outgoing edges.
    transitions.put(IssueStatus.CLOSED, EnumSet.noneOf(IssueStatus.class));
    transitions.put(IssueStatus.REJECTED, EnumSet.noneOf(IssueStatus.class));

    List<AuthorizationRule> rules = new ArrayList<>();
    // Order matters: the REJECTED check ran first in the legacy code.
    rules.add(new ManageAuthorizationRule(
        (from, to) -> to == IssueStatus.REJECTED,
        "Only PM/Admin can mark an issue as REJECTED"));
    rules.add(new ManageAuthorizationRule(
        (from, to) -> from == IssueStatus.MARK_REJECTED,
        "Only PM/Admin can review MARK_REJECTED issues"));

    return new WorkflowDefinition(transitions, rules);
  }
}
