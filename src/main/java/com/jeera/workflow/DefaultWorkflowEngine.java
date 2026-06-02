package com.jeera.workflow;

import com.jeera.model.enums.IssueStatus;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkflowEngine implements WorkflowEngine {

  private final WorkflowDefinition definition;

  public DefaultWorkflowEngine(WorkflowProvider workflowProvider) {
    this.definition = workflowProvider.getWorkflow();
  }

  @Override
  public void validateTransition(TransitionContext context) {
    for (AuthorizationRule rule : definition.authorizationRules()) {
      if (rule.appliesTo(context.from(), context.to())) {
        rule.enforce(context);
      }
    }
    requireValidPath(context.from(), context.to());
  }

  @Override
  public void requireValidPath(IssueStatus from, IssueStatus to) {
    if (from == to) {
      return;
    }
    if (!definition.allowedTransitions(from).contains(to)) {
      throw new IllegalStateException("Invalid issue status transition: " + from + " -> " + to);
    }
  }

  @Override
  public Set<IssueStatus> allowedNextStatuses(IssueStatus from) {
    return definition.allowedTransitions(from);
  }
}
