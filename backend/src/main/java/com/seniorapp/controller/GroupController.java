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

    // --- ISSUE #72: GRUP KURMA (POST /api/groups) ---
    @PostMapping
    public ResponseEntity<String> createGroup(@RequestBody GroupCreateDto requestDto) {
        groupService.createGroup(requestDto.getStudentId(), requestDto.getGroupName(), requestDto.getProjectId());
        return new ResponseEntity<>("{\"message\": \"Group created successfully\"}", HttpStatus.CREATED);
    }

    // --- ISSUE #74: ÜYE YÖNETİMİ (PATCH /api/groups/{groupId}/members) ---
    @PatchMapping("/{groupId}/members")
    public ResponseEntity<String> manageMembers(
            @PathVariable Long groupId, 
            @RequestBody GroupMemberActionDto actionDto) {
        
        groupService.manageMembership(groupId, actionDto);
        
        // Ekleme mi çıkarma mı yapıldığına göre dinamik mesaj döner
        String actionDone = actionDto.getAction().toLowerCase() + "ed"; 
        return ResponseEntity.ok("{\"message\": \"Member " + actionDone + " successfully\"}");
    }
}