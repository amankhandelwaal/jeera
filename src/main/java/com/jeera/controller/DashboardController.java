package com.jeera.controller;

import com.jeera.model.User;
import com.jeera.model.enums.UserRole;
import com.jeera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final UserService userService;

  @GetMapping
  public String dashboard(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return "redirect:/login";
    }

    User user = userService.findByUsername(authentication.getName());
    if (user.getSystemRole() == UserRole.ADMIN) {
      return "redirect:/admin/users";
    }

    if (user.getSystemRole() == UserRole.USER) {
      return "dashboard/user";
    }

    return "dashboard/user";
  }
}
