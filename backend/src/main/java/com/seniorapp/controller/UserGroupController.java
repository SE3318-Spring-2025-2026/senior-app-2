package com.seniorapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.seniorapp.dto.ForceGroupTransferRequest;
import com.seniorapp.dto.GroupTransferResponse;
import com.seniorapp.service.UserGroupService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class UserGroupController {
  private static final Logger log = LoggerFactory.getLogger(UserGroupController.class);
  private final UserGroupService userGroupService;

  public UserGroupController(UserGroupService userGroupService) {
    this.userGroupService = userGroupService;
  }

  @PostMapping("/api/admin/groups/{groupId}/transfer")
  public ResponseEntity<GroupTransferResponse> forceTransferGroup(@Valid @RequestBody ForceGroupTransferRequest request) {
    log.debug("Processing request to transfer group");

    try {
      GroupTransferResponse body = userGroupService.updateUserGroup(request.getGroup(), request.getCoordinator());

      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
  }
}
