package com.jeera.dto;

import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.IssueType;
import lombok.Data;

@Data
public class IssueFilterDto {

  private IssueStatus status;
  private IssuePriority priority;
  private IssueType type;
}
