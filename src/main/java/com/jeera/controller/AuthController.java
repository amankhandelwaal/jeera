package com.jeera.controller;

import com.jeera.model.User;
import com.jeera.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;

  @GetMapping("login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("admin/users/new")
  public String registerForm(Model model) {
    model.addAttribute("user", new User());
    return "auth/register";
  }

  @PostMapping("admin/users/new")
  public String registerUser(
      @Valid User user,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("user", user);
      return "auth/register";
    }

    userService.registerUser(user);
    redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
    return "redirect:/admin/users";
  }
}
