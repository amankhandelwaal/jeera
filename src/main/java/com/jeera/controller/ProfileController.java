package com.jeera.controller;

import com.jeera.model.User;
import com.jeera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final UserService userService;

  @GetMapping
  public String profile(Authentication authentication, Model model) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }

    User user = userService.findByUsername(authentication.getName());
    model.addAttribute("user", user);
    return "profile";
  }

  @PostMapping
  public String updateProfile(
      @RequestParam String username,
      @RequestParam String email,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Unauthorized");
    }

    User actor = userService.findByUsername(authentication.getName());
    try {
      userService.updateProfile(actor.getId(), username, email);
      redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
    } catch (IllegalStateException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/profile";
  }
}
