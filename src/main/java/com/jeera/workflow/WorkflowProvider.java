package com.jeera.workflow;

/**
 * Supplies the {@link WorkflowDefinition} used by the engine. A single default implementation
 * exists today; a future per-project provider can be dropped in without touching the engine or
 * services.
 */
public interface WorkflowProvider {

  WorkflowDefinition getWorkflow();
}
