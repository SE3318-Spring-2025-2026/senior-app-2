package com.seniorapp.controller;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Issue #72: Grup Kurma
    @PostMapping
    public ResponseEntity<String> createGroup(@RequestBody GroupCreateDto requestDto) {
        groupService.createGroup(requestDto);
        return new ResponseEntity<>("{\"message\": \"Group created and linked to project successfully.\"}", HttpStatus.CREATED);
    }

    // Issue #74: Üye Yönetimi (Conflict çözümü için burada durmalı)
    @PatchMapping("/{groupId}/members")
    public ResponseEntity<String> manageMembers(
            @PathVariable Long groupId,
            @RequestBody GroupMemberActionDto actionDto) {
        
        groupService.manageMembership(groupId, actionDto);
        return ResponseEntity.ok("{\"message\": \"Member action processed successfully\"}");
    }
}