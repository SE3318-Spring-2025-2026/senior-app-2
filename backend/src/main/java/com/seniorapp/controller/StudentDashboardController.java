package com.seniorapp.controller;

import com.seniorapp.dto.student.StudentDashboardDtos.DashboardResponse;
import com.seniorapp.dto.student.StudentDashboardDtos.ActiveProjectItem;
import com.seniorapp.entity.User;
import com.seniorapp.service.StudentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/students/dashboard")
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;

    public StudentDashboardController(StudentDashboardService studentDashboardService) {
        this.studentDashboardService = studentDashboardService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DashboardResponse> getMyDashboard(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(new DashboardResponse("success", studentDashboardService.getDashboard(principal.getId())));
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ActiveProjectItem>> getMyProjects(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(studentDashboardService.getActiveProjects(principal.getId()));
    }
}
