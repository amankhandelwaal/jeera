package com.jeera.controller;

import com.jeera.model.Notification;
import com.jeera.model.User;
import com.jeera.service.NotificationService;
import com.jeera.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final UserService userService;

  @GetMapping
  public String notifications(Authentication authentication, Model model) {
    User user = userService.findByUsername(authentication.getName());
    List<Notification> notifications =
        notificationService.getNotificationsByRecipientId(user.getId());
    model.addAttribute("notifications", notifications);
    return "notifications/list";
  }

  @GetMapping("/unread-count")
  @ResponseBody
  public long unreadCount(Authentication authentication) {
    User user = userService.findByUsername(authentication.getName());
    return notificationService.getUnreadCount(user.getId());
  }

  @PostMapping("/mark-read")
  public String markAllRead(Authentication authentication, RedirectAttributes redirectAttributes) {
    User user = userService.findByUsername(authentication.getName());
    notificationService.markAllRead(user.getId());
    redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read");
    return "redirect:/notifications";
  }
}
