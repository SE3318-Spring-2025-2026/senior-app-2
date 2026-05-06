package com.seniorapp.service;

import com.seniorapp.dto.analytics.AnalyticsDtos.*;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final SubmissionGradeRepository submissionGradeRepository;

    public AnalyticsService(
            UserRepository userRepository,
            UserGroupRepository userGroupRepository,
            UserGroupMemberRepository userGroupMemberRepository,
            SubmissionGradeRepository submissionGradeRepository
    ) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.submissionGradeRepository = submissionGradeRepository;
    }

    @Transactional(readOnly = true)
    public StudentPerformanceResponse getStudentPerformance(Long studentId) {
        StudentPerformanceResponse response = new StudentPerformanceResponse();
        
        // Get student's grades
        List<SubmissionGrade> grades = submissionGradeRepository.findAll().stream()
                .filter(g -> {
                    User student = g.getSubmission() != null ? 
                            userRepository.findById(g.getSubmission().getSubmittedByUserId()).orElse(null) : null;
                    return student != null && student.getId().equals(studentId);
                })
                .collect(Collectors.toList());

        // Calculate metrics (placeholder - replace with actual calculations)
        List<MetricItem> metrics = new ArrayList<>();
        double avgGrade = grades.stream()
                .mapToDouble(SubmissionGrade::getGrade)
                .average()
                .orElse(0.0);
        
        metrics.add(new MetricItem("Accuracy", avgGrade * 0.9));
        metrics.add(new MetricItem("Speed", avgGrade * 0.85));
        metrics.add(new MetricItem("Quality", avgGrade * 0.95));
        metrics.add(new MetricItem("Consistency", avgGrade * 0.88));
        metrics.add(new MetricItem("Participation", avgGrade * 0.92));
        
        response.setMetrics(metrics);

        // Calculate summary stats
        SummaryStats summary = new SummaryStats();
        summary.setAverageScore(avgGrade);
        summary.setMaxScore(grades.stream().mapToDouble(SubmissionGrade::getGrade).max().orElse(0.0));
        summary.setMinScore(grades.stream().mapToDouble(SubmissionGrade::getGrade).min().orElse(0.0));
        summary.setImprovement(calculateImprovement(grades));
        response.setSummary(summary);

        return response;
    }

    @Transactional(readOnly = true)
    public GroupPerformanceResponse getGroupPerformance(Long groupId) {
        GroupPerformanceResponse response = new GroupPerformanceResponse();
        
        // Get group's grades
        List<SubmissionGrade> grades = submissionGradeRepository.findAll().stream()
                .filter(g -> g.getSubmission() != null && g.getSubmission().getGroupId().equals(groupId))
                .collect(Collectors.toList());

        // Calculate metrics
        List<MetricItem> metrics = new ArrayList<>();
        double avgGrade = grades.stream()
                .mapToDouble(SubmissionGrade::getGrade)
                .average()
                .orElse(0.0);
        
        metrics.add(new MetricItem("Team Collaboration", avgGrade * 0.9));
        metrics.add(new MetricItem("Code Quality", avgGrade * 0.88));
        metrics.add(new MetricItem("Documentation", avgGrade * 0.85));
        metrics.add(new MetricItem("Meeting Deadlines", avgGrade * 0.92));
        metrics.add(new MetricItem("Communication", avgGrade * 0.9));
        
        response.setMetrics(metrics);

        // Calculate group summary
        GroupSummaryStats summary = new GroupSummaryStats();
        summary.setAverageScore(avgGrade);
        summary.setMemberCount(userGroupMemberRepository.findByGroupId(groupId).size());
        summary.setProjectCount(1); // Placeholder - calculate from actual projects
        response.setSummary(summary);

        return response;
    }

    @Transactional(readOnly = true)
    public List<TrendItem> getPerformanceTrends(Long studentId, Long groupId, String startDate, String endDate) {
        List<TrendItem> trends = new ArrayList<>();
        
        // Generate trend data (placeholder - replace with actual data from submissions)
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(3);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        LocalDate current = start;
        int week = 1;
        while (!current.isAfter(end)) {
            double score = 70 + (Math.random() * 25); // Placeholder score
            trends.add(new TrendItem(
                current.format(DateTimeFormatter.ISO_DATE),
                score,
                "Week " + week
            ));
            current = current.plusWeeks(1);
            week++;
        }
        
        return trends.stream()
                .sorted(Comparator.comparing(TrendItem::getDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailableStudent> getAvailableStudents() {
        return userRepository.findByRole(Role.STUDENT).stream()
                .map(user -> new AvailableStudent(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getGithubUsername() // Using githubUsername as studentId placeholder
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailableGroup> getAvailableGroups() {
        return userGroupRepository.findAll().stream()
                .map(group -> new AvailableGroup(
                        group.getId(),
                        group.getGroupName()
                ))
                .collect(Collectors.toList());
    }

    private double calculateImprovement(List<SubmissionGrade> grades) {
        if (grades.size() < 2) return 0.0;
        
        List<SubmissionGrade> sortedGrades = grades.stream()
                .sorted(Comparator.comparing(g -> g.getSubmission().getSubmittedAt()))
                .collect(Collectors.toList());
        
        double firstHalfAvg = sortedGrades.subList(0, sortedGrades.size() / 2).stream()
                .mapToDouble(SubmissionGrade::getGrade)
                .average()
                .orElse(0.0);
        
        double secondHalfAvg = sortedGrades.subList(sortedGrades.size() / 2, sortedGrades.size()).stream()
                .mapToDouble(SubmissionGrade::getGrade)
                .average()
                .orElse(0.0);
        
        return secondHalfAvg - firstHalfAvg;
    }
}
