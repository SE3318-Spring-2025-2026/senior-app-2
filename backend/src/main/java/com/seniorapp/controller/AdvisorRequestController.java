package com.seniorapp.controller;

import com.seniorapp.entity.AdvisorRequest;
import com.seniorapp.service.AdvisorRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advisor-requests")
@RequiredArgsConstructor
public class AdvisorRequestController {

    private final AdvisorRequestService advisorRequestService;

    @PostMapping("/invite-committee/{groupId}")
    public ResponseEntity<String> invite(@PathVariable Long groupId) {
        advisorRequestService.createInvitesForCommittee(groupId);
        return ResponseEntity.ok("Invites sent successfully");
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<AdvisorRequest>> getMyRequests() {
        return ResponseEntity.ok(advisorRequestService.getRequestsForCurrentProfessor());
    }

    @PostMapping("/{requestId}/respond")
    public ResponseEntity<String> respond(@PathVariable Long requestId, @RequestParam String decision) {
        advisorRequestService.processDecision(requestId, decision);
        return ResponseEntity.ok("Decision processed");
    }
}