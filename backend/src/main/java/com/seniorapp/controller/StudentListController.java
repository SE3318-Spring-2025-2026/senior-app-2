package com.seniorapp.controller;

import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentListController {

    private final UserRepository userRepository;

    public StudentListController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listStudents() {
        List<Map<String, Object>> students = userRepository.findByRole(Role.STUDENT).stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "fullName", (Object) (user.getFullName() != null ? user.getFullName() : "—"),
                        "email", (Object) (user.getEmail() != null ? user.getEmail() : "—"),
                        "githubUsername", (Object) (user.getGithubUsername() != null ? user.getGithubUsername() : ""),
                        "githubId", (Object) (user.getGithubId() != null ? user.getGithubId() : 0),
                        "enabled", (Object) user.isEnabled()
                ))
                .toList();
        return ResponseEntity.ok(students);
    }
}
