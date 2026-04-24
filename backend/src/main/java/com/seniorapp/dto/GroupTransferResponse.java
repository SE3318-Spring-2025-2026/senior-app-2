package com.seniorapp.dto;

import com.seniorapp.entity.User;

import jakarta.validation.constraints.NotNull;

public class GroupTransferResponse {
  private String message;

  @NotNull
  private User coordinator;

  public GroupTransferResponse() {
  }

  public GroupTransferResponse(String message, User coordinator) {
    this.message = message;
    this.coordinator = coordinator;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public User getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(User coordinator) {
    this.coordinator = coordinator;
  }
}