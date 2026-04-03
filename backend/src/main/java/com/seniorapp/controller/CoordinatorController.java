package com.seniorapp.controller;

import com.seniorapp.entity.StudentWhitelist;
import com.seniorapp.repository.StudentWhitelistRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorController {

    private final StudentWhitelistRepository whitelistRepository;

    public CoordinatorController(StudentWhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    @PostMapping("/whitelist")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> uploadWhitelist(@RequestBody Map<String, List<String>> request) {
        List<String> studentIds = request.get("studentIds");
        
        if (studentIds == null || studentIds.isEmpty()) {
            return ResponseEntity.badRequest().body("ID list is empty");
        }

        List<StudentWhitelist> entities = studentIds.stream()
                .filter(id -> !whitelistRepository.existsByStudentId(id))
                .map(StudentWhitelist::new)
                .collect(Collectors.toList());

        whitelistRepository.saveAll(entities);

        return ResponseEntity.ok(Map.of("message", entities.size() + " new students whitelisted."));
    }
}