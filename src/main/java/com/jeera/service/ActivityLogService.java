package com.jeera.service;

import com.jeera.model.ActivityLog;
import com.jeera.model.Issue;
import com.jeera.model.User;
import com.jeera.repository.ActivityLogRepository;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

  private final ActivityLogRepository activityLogRepository;
  private final IssueRepository issueRepository;
  private final UserRepository userRepository;

  public ActivityLog createActivityLog(Issue issue, User actor, String action, String oldValue, String newValue) {
    Issue resolvedIssue = issueRepository.findById(issue.getId())
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issue.getId()));
    User resolvedActor = userRepository.findById(actor.getId())
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + actor.getId()));

    ActivityLog activityLog = ActivityLog.builder()
        .issue(resolvedIssue)
        .actor(resolvedActor)
        .action(action)
        .oldValue(oldValue)
        .newValue(newValue)
        .timestamp(LocalDateTime.now())
        .build();

    return activityLogRepository.save(activityLog);
  }
}
