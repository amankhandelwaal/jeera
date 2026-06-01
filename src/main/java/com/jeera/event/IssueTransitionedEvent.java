package com.jeera.event;

import com.jeera.model.enums.IssueStatus;

/** Published after an issue's status has changed. */
public record IssueTransitionedEvent(Long issueId, IssueStatus oldStatus, IssueStatus newStatus, Long actorId) {
}
