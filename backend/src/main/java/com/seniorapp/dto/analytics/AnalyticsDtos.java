package com.seniorapp.dto.analytics;

import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    // Student Performance Response
    public static class StudentPerformanceResponse {
        private List<MetricItem> metrics;
        private SummaryStats summary;

        public StudentPerformanceResponse() {}

        public List<MetricItem> getMetrics() { return metrics; }
        public void setMetrics(List<MetricItem> metrics) { this.metrics = metrics; }
        public SummaryStats getSummary() { return summary; }
        public void setSummary(SummaryStats summary) { this.summary = summary; }
    }

    // Group Performance Response
    public static class GroupPerformanceResponse {
        private List<MetricItem> metrics;
        private GroupSummaryStats summary;

        public GroupPerformanceResponse() {}

        public List<MetricItem> getMetrics() { return metrics; }
        public void setMetrics(List<MetricItem> metrics) { this.metrics = metrics; }
        public GroupSummaryStats getSummary() { return summary; }
        public void setSummary(GroupSummaryStats summary) { this.summary = summary; }
    }

    // Metric Item (for radar chart)
    public static class MetricItem {
        private String metric;
        private Double value;

        public MetricItem() {}

        public MetricItem(String metric, Double value) {
            this.metric = metric;
            this.value = value;
        }

        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
    }

    // Summary Stats for Student
    public static class SummaryStats {
        private Double averageScore;
        private Double maxScore;
        private Double minScore;
        private Double improvement;

        public SummaryStats() {}

        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
        public Double getMaxScore() { return maxScore; }
        public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }
        public Double getMinScore() { return minScore; }
        public void setMinScore(Double minScore) { this.minScore = minScore; }
        public Double getImprovement() { return improvement; }
        public void setImprovement(Double improvement) { this.improvement = improvement; }
    }

    // Summary Stats for Group
    public static class GroupSummaryStats {
        private Double averageScore;
        private Integer memberCount;
        private Integer projectCount;

        public GroupSummaryStats() {}

        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
        public Integer getMemberCount() { return memberCount; }
        public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
        public Integer getProjectCount() { return projectCount; }
        public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    }

    // Performance Trend Item
    public static class TrendItem {
        private String date;
        private Double score;
        private String label;

        public TrendItem() {}

        public TrendItem(String date, Double score, String label) {
            this.date = date;
            this.score = score;
            this.label = label;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    // Available Student for filtering
    public static class AvailableStudent {
        private Long id;
        private String name;
        private String email;
        private String studentId;

        public AvailableStudent() {}

        public AvailableStudent(Long id, String name, String email, String studentId) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.studentId = studentId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
    }

    // Available Group for filtering
    public static class AvailableGroup {
        private Long id;
        private String name;

        public AvailableGroup() {}

        public AvailableGroup(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
