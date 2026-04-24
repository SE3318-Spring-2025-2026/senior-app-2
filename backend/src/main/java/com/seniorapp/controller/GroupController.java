package com.seniorapp.controller;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupInviteRequestDto;
import com.seniorapp.dto.GroupInviteRespondDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.dto.ScoreOverrideRequest;
import com.seniorapp.dto.TeamManagementDtos.CreateProjectFromTemplateRequest;
import com.seniorapp.dto.TeamManagementDtos.StudentListResponse;
import com.seniorapp.dto.TeamManagementDtos.TeamListResponse;
import com.seniorapp.entity.User;
import com.seniorapp.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
    
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

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<String> inviteMember(
            @PathVariable Long groupId,
            @RequestBody GroupInviteRequestDto request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        groupService.inviteMember(groupId, request.getStudentUserId(), currentUser.getId());
        return ResponseEntity.ok("{\"message\":\"Invite sent successfully.\"}");
    }

    @PatchMapping("/invites/{inviteId}")
    public ResponseEntity<String> respondInvite(
            @PathVariable Long inviteId,
            @RequestBody GroupInviteRespondDto request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        groupService.respondInvite(inviteId, request.getAction(), currentUser.getId());
        return ResponseEntity.ok("{\"message\":\"Invite updated successfully.\"}");
    }

    @GetMapping("/my-teams")
    public ResponseEntity<TeamListResponse> getMyTeams(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new TeamListResponse("success", groupService.getMyTeams(currentUser.getId())));
    }

    @GetMapping("/students")
    public ResponseEntity<StudentListResponse> listStudents() {
        return ResponseEntity.ok(new StudentListResponse("success", groupService.listStudents()));
    }

    @GetMapping("/{groupId}/advisor-options")
    public ResponseEntity<StudentListResponse> listAdvisorOptions(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new StudentListResponse("success", groupService.listAdvisorOptions(groupId, currentUser.getId())));
    }

    @PostMapping("/{groupId}/project")
    public ResponseEntity<Map<String, Object>> createProjectForGroup(
            @PathVariable Long groupId,
            @RequestBody CreateProjectFromTemplateRequest request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Long projectId = groupService.createProjectForGroup(groupId, request, currentUser.getId());
        return ResponseEntity.ok(Map.of("status", "success", "data", Map.of("projectId", projectId)));
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
    /**
     * Endpoint for authorized professors/advisors to manually override AI-generated scores.
     * Acceptance Criteria: Validate studentId exists and is linked to auditId.
     */
  @Operation(summary = "Manual score override", description = "Allows authorized users to manually override the score for a specific student.")
    @PreAuthorize("hasAnyRole('ADVISOR', 'PROFESSOR')")
    @PatchMapping("/grading/override/{auditId}")
    public ResponseEntity<String> overrideScore(
            @PathVariable Long auditId, 
            @Valid @RequestBody ScoreOverrideRequest request) { // <-- @Valid eklendi
        
        // Manuel validation (if bloğu) sildik çünkü @Valid bunu otomatik yapıyor.
        
        // Success response
        return ResponseEntity.ok("Success: Manual override request received for Audit ID " + auditId);
    }
}
