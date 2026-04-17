package com.jeera.dto;

import com.jeera.model.enums.ProjectRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberDto {

  @NotBlank(message = "Username is required")
  private String username;

  @NotNull(message = "Project role is required")
  private ProjectRole projectRole;
}
