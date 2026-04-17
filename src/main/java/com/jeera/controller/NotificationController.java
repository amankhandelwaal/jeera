package com.jeera.controller;

import com.jeera.model.User;
import com.jeera.service.NotificationService;
import com.jeera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final UserService userService;

  @GetMapping("/unread-count")
  public long unreadCount(Authentication authentication) {
    User user = userService.findByUsername(authentication.getName());
    return notificationService.getUnreadCount(user.getId());
  }
}
