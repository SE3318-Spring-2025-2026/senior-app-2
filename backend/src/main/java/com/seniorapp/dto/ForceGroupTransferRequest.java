package com.seniorapp.dto;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForceGroupTransferRequest {
  @NotBlank
  private UserGroup group;

  @NotBlank
  private User coordinator;
}
