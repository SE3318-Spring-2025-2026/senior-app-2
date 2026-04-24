package com.seniorapp.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.seniorapp.dto.GroupTransferResponse;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.UserGroupRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UserGroupService {
  private static final Logger log = LoggerFactory.getLogger(UserGroupService.class);
  private final UserGroupRepository userGroups;

  public UserGroupService(UserGroupRepository userGroups) {
    this.userGroups = userGroups;
  }

  // Force update the group ownership if the requstor is an admin
  public GroupTransferResponse updateUserGroup(UserGroup group, User user) throws Exception {
    // First verify role
    log.atDebug().setMessage("Received force update group request. Validating permissions.").log();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User requestor = (User) auth.getPrincipal();
    if (requestor.getRole() != Role.ADMIN) {
      throw new Exception("Not authorized");
    }

    // Then forcibly update the group
    log.atInfo().setMessage("Forcibly updating ownership of {} group to {}").addArgument(group.getId()).addArgument(user.getFullName()).log();
    group.setCoordinator(user);
    userGroups.save(group);

    return new GroupTransferResponse("Group has been manually transferred to new advisor.", user);
  }
}
