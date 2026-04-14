package com.seniorapp.controller;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    private final GroupService groupService;
    public GroupController(GroupService groupService) { this.groupService = groupService; }

    @PostMapping
    public ResponseEntity<String> createGroup(@RequestBody GroupCreateDto requestDto) {
        groupService.createGroup(requestDto.getStudentId(), requestDto.getGroupName(), requestDto.getProjectId());
        return new ResponseEntity<>("{\"message\": \"Group created successfully\"}", HttpStatus.CREATED);
    }
}
