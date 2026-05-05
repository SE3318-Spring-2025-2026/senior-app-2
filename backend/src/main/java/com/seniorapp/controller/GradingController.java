package com.seniorapp.controller;

import com.seniorapp.dto.ScoreOverrideRequest;
import com.seniorapp.service.GradingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/grading")
public class GradingController {

    @Autowired
    private GradingService gradingService;

    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
    @PatchMapping("/override/{auditId}")
    public ResponseEntity<?> overrideScore(
            @PathVariable Long auditId, // Burası da UUID yerine Long oldu
            @Valid @RequestBody ScoreOverrideRequest request) {
        
        gradingService.overrideScore(auditId, request);
        return ResponseEntity.ok().body("Score updated successfully.");
    }
}