package com.seniorapp.controller;

import com.seniorapp.dto.analytics.AnalyticsDtos.*;
import com.seniorapp.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get performance metrics for a specific student
     */
    @GetMapping("/students/{studentId}/performance")
    public ResponseEntity<StudentPerformanceResponse> getStudentPerformance(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(analyticsService.getStudentPerformance(studentId));
    }

    /**
     * Get performance metrics for a specific group
     */
    @GetMapping("/groups/{groupId}/performance")
    public ResponseEntity<GroupPerformanceResponse> getGroupPerformance(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(analyticsService.getGroupPerformance(groupId));
    }

    /**
     * Get performance trends by time series
     * Query params: studentId, groupId, startDate, endDate
     */
    @GetMapping("/performance-trends")
    public ResponseEntity<List<TrendItem>> getPerformanceTrends(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(analyticsService.getPerformanceTrends(studentId, groupId, startDate, endDate));
    }

    /**
     * Get available students for analytics filtering
     */
    @GetMapping("/available-students")
    public ResponseEntity<List<AvailableStudent>> getAvailableStudents() {
        return ResponseEntity.ok(analyticsService.getAvailableStudents());
    }

    /**
     * Get available groups for analytics filtering
     */
    @GetMapping("/available-groups")
    public ResponseEntity<List<AvailableGroup>> getAvailableGroups() {
        return ResponseEntity.ok(analyticsService.getAvailableGroups());
    }
}
