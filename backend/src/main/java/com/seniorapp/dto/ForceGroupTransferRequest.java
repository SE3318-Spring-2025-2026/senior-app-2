package com.seniorapp.dto;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;

import jakarta.validation.constraints.NotNull;

public class ForceGroupTransferRequest {
  @NotNull
  private UserGroup group;

  @NotNull
  private User coordinator;

  public UserGroup getGroup() {
    return group;
  }

  public void setGroup(UserGroup group) {
    this.group = group;
  }

  public User getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(User coordinator) {
    this.coordinator = coordinator;
  }
}
