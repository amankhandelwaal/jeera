package com.jeera.service;

import com.jeera.model.Issue;
import com.jeera.model.Notification;
import com.jeera.model.User;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.NotificationRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final IssueRepository issueRepository;

  public Notification createNotification(User recipient, String message, Issue issue) {
    User resolvedRecipient = userRepository.findById(recipient.getId())
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + recipient.getId()));
    Issue resolvedIssue = issueRepository.findById(issue.getId())
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issue.getId()));

    Notification notification = Notification.builder()
        .recipient(resolvedRecipient)
        .message(message)
        .issue(resolvedIssue)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    return notificationRepository.save(notification);
  }

  public long getUnreadCount(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
  }
}
