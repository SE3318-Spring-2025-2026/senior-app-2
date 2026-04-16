package com.seniorapp.controller;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.service.GroupService;
import jakarta.validation.Valid;
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

    // --- ISSUE #72: GRUP KURMA (SENİN KODUN) ---
    @PostMapping
    public ResponseEntity<String> createGroup(@RequestBody GroupCreateDto requestDto) {
        groupService.createGroup(requestDto);
        return new ResponseEntity<>("{\"message\": \"Group created and linked to project successfully.\"}", HttpStatus.CREATED);
    }

    // --- ISSUE #74: ÜYE YÖNETİMİ (MAIN'DEN GELEN) ---
    @PatchMapping("/{groupId}/members")
    public ResponseEntity<String> manageMembers(
            @PathVariable Long groupId, 
            @RequestBody GroupMemberActionDto actionDto) {
        
        groupService.manageMembership(groupId, actionDto);
        
        String actionDone = actionDto.getAction().toLowerCase() + "ed"; 
        return ResponseEntity.ok("{\"message\": \"Member " + actionDone + " successfully\"}");
    }

    @PostMapping("/{groupId}/integrations")
    public ResponseEntity<String> saveIntegrations(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupIntegrationsRequest request) {
        groupService.saveIntegrations(groupId, request);
        return ResponseEntity.ok("{\"message\":\"Integrations saved successfully.\"}");
    }

    @GetMapping("/{groupId}/integrations")
    public ResponseEntity<GroupIntegrationsResponse> getIntegrations(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getIntegrations(groupId));
    }
}