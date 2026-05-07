package com.seniorapp.controller;

import com.seniorapp.dto.AdvisorDecisionDto;
import com.seniorapp.dto.AdvisorRequestDto;
import com.seniorapp.service.AdvisorRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/advisor-requests")
@CrossOrigin(origins = "*")
public class AdvisorRequestController {

    private final AdvisorRequestService advisorRequestService;

    public AdvisorRequestController(AdvisorRequestService advisorRequestService) {
        this.advisorRequestService = advisorRequestService;
    }

    @PostMapping
    public ResponseEntity<String> sendRequest(@RequestBody AdvisorRequestDto requestDto) {
        advisorRequestService.createRequest(requestDto.getGroupId(), requestDto.getProfessorId());
        return new ResponseEntity<>("Request created successfully.", HttpStatus.CREATED);
    }

    @PutMapping("/decision")
    public ResponseEntity<String> handleDecision(@RequestBody AdvisorDecisionDto decisionDto) {
        advisorRequestService.processDecision(
            decisionDto.getRequestId(), 
            decisionDto.getCurrentProfessorId(), 
            decisionDto.getDecision()
        );
        return ResponseEntity.ok("Decision processed.");
    }
}