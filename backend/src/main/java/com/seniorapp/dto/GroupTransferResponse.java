package com.seniorapp.dto;

import com.seniorapp.entity.User;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupTransferResponse {
  private String message;

  @NotBlank
  private User coordinator;
}