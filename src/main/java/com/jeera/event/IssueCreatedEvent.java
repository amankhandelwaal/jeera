package com.jeera.event;

/** Published after a new issue has been persisted. */
public record IssueCreatedEvent(Long issueId) {}
