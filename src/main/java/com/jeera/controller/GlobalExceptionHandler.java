package com.jeera.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public String handleEntityNotFound(
      EntityNotFoundException ex, Model model, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    model.addAttribute("errorTitle", "Resource Not Found");
    model.addAttribute("errorMessage", ex.getMessage());
    return "error/404";
  }

  @ExceptionHandler(AccessDeniedException.class)
  public String handleAccessDenied(
      AccessDeniedException ex, Model model, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    model.addAttribute("errorTitle", "Access Denied");
    model.addAttribute("errorMessage", "You do not have permission to perform this action.");
    return "error/403";
  }

  @ExceptionHandler(IllegalStateException.class)
  public String handleIllegalState(
      IllegalStateException ex, Model model, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    model.addAttribute("errorTitle", "Invalid Operation");
    model.addAttribute("errorMessage", ex.getMessage());
    return "error/400";
  }

  @ExceptionHandler(Exception.class)
  public String handleGeneric(Exception ex, Model model, HttpServletResponse response) {
    LOGGER.error("Unhandled exception", ex);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    model.addAttribute("errorTitle", "Something Went Wrong");
    model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
    return "error/500";
  }
}
