package com.jeera.dto;

import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateIssueDto {

  @NotBlank(message = "Title is required")
  private String title;

  @NotBlank(message = "Description is required")
  private String description;

  @NotNull(message = "Type is required")
  private IssueType type;

  @NotNull(message = "Priority is required")
  private IssuePriority priority;

  @NotNull(message = "Project id is required")
  private Long projectId;
}
