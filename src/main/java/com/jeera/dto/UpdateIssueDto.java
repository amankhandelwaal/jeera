package com.jeera.dto;

import com.jeera.model.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateIssueDto {

  @NotNull(message = "Status is required")
  private IssueStatus status;
}
