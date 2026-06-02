package com.jeera.event;

/** Published after an issue has been assigned to a developer. */
public record IssueAssignedEvent(Long issueId, Long assigneeId, Long actorId) {}
