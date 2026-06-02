package com.jeera.service;

import com.jeera.model.Issue;
import com.jeera.model.Notification;
import com.jeera.model.Project;
import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.NotificationRepository;
import com.jeera.repository.ProjectRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final IssueRepository issueRepository;
  private final ProjectRepository projectRepository;

  public Notification createNotification(User recipient, String message, Issue issue) {
    User resolvedRecipient =
        userRepository
            .findById(recipient.getId())
            .orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + recipient.getId()));
    Issue resolvedIssue =
        issueRepository
            .findById(issue.getId())
            .orElseThrow(
                () -> new EntityNotFoundException("Issue not found with id: " + issue.getId()));

    Notification notification =
        Notification.builder()
            .recipient(resolvedRecipient)
            .message(message)
            .issue(resolvedIssue)
            .project(null)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

    return notificationRepository.save(notification);
  }

  public Notification createProjectNotification(User recipient, String message, Project project) {
    User resolvedRecipient =
        userRepository
            .findById(recipient.getId())
            .orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + recipient.getId()));
    Project resolvedProject =
        projectRepository
            .findById(project.getId())
            .orElseThrow(
                () -> new EntityNotFoundException("Project not found with id: " + project.getId()));

    Notification notification =
        Notification.builder()
            .recipient(resolvedRecipient)
            .message(message)
            .issue(null)
            .project(resolvedProject)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

    return notificationRepository.save(notification);
  }

  public Notification createAdminNotification(User recipient, String message) {
    User resolvedRecipient =
        userRepository
            .findById(recipient.getId())
            .orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + recipient.getId()));

    Notification notification =
        Notification.builder()
            .recipient(resolvedRecipient)
            .message(message)
            .issue(null)
            .project(null)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

    return notificationRepository.save(notification);
  }

  public void notifyActiveAdmins(String message) {
    List<User> admins = userRepository.findBySystemRoleAndIsActiveTrue(UserRole.ADMIN);
    for (User admin : admins) {
      createAdminNotification(admin, message);
    }
  }

  public void notifyActiveAdmins(String message, Project project) {
    List<User> admins = userRepository.findBySystemRoleAndIsActiveTrue(UserRole.ADMIN);
    for (User admin : admins) {
      createProjectNotification(admin, message, project);
    }
  }

  public long getUnreadCount(Long userId) {
    userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
  }

  public List<Notification> getNotificationsByRecipientId(Long userId) {
    userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    return notificationRepository.findPageFeedByRecipientId(userId);
  }

  public void markAllRead(Long userId) {
    userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    List<Notification> unread =
        notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    for (Notification notification : unread) {
      notification.setRead(true);
    }
    notificationRepository.saveAll(unread);
  }
}
