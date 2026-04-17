package com.seniorapp.controller;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.entity.User;
import com.seniorapp.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // --- ISSUE #71 & #72: GRUP KURMA ---
    @PostMapping
    public ResponseEntity<String> createGroup(@RequestBody GroupCreateDto requestDto, Authentication authentication) {
        //  CRITICAL FIX: Token'dan giriş yapan kullanıcının ID'sini alıyoruz
        // User entity'nizden ID'yi çekiyoruz (Sistemdeki Security kurgunuza göre uyarla)
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        groupService.createGroup(requestDto, currentUserId);
        
        return new ResponseEntity<>("{\"message\": \"Group created and linked to project successfully.\"}", HttpStatus.CREATED);
    }

    // --- ISSUE #73 & #74: ÜYE YÖNETİMİ ---
    @PatchMapping("/{groupId}/members")
    public ResponseEntity<String> manageMembers(
            @PathVariable Long groupId, 
            @RequestBody GroupMemberActionDto actionDto,
            Authentication authentication) {
        
        // 🚀 CRITICAL FIX: İşlemi yapanın kim olduğunu servise gönderiyoruz ki LİDER mi kontrol edebilsin
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        groupService.manageMembership(groupId, actionDto, currentUserId);
        
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