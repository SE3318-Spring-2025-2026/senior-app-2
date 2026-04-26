package com.seniorapp.service;

import com.seniorapp.dto.comparison.ComparisonDtos.*;
import com.seniorapp.entity.Project;
import com.seniorapp.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ComparisonService {

    private final ProjectRepository projectRepository;

    public ComparisonService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public ComparisonResponse getComparisonData(Long projectId) {
        ComparisonResponse response = new ComparisonResponse();
        
        // Get project
        Project project = projectRepository.findById(projectId).orElse(null);
        
        // Placeholder Jira requirement
        JiraRequirement requirement = new JiraRequirement();
        requirement.setId("REQ-" + projectId);
        requirement.setKey("PROJ-" + projectId);
        requirement.setSummary(project != null ? project.getTitle() : "Sample Requirement");
        requirement.setDescription("This is a placeholder Jira requirement description. In production, this would be fetched from Jira API.");
        requirement.setAcceptanceCriteria(List.of(
            "Code must follow coding standards",
            "All tests must pass",
            "Documentation must be updated"
        ));
        requirement.setPriority("High");
        requirement.setStatus("In Progress");
        requirement.setAssignee("Team Lead");
        response.setRequirement(requirement);

        // Placeholder diff
        String sampleDiff = """
diff --git a/src/main/java/com/example/Service.java b/src/main/java/com/example/Service.java
index 1234567..abcdefg 100644
--- a/src/main/java/com/example/Service.java
+++ b/src/main/java/com/example/Service.java
@@ -10,5 +10,7 @@ public class Service {
     public void processData() {
-        System.out.println("Processing data");
+        logger.info("Processing data");
+        validateInput();
     }
+    
+    private void validateInput() {
+        if (input == null) {
+            throw new IllegalArgumentException("Input cannot be null");
+        }
+    }
 }
""";
        response.setDiff(sampleDiff);

        // Placeholder AI feedback
        List<AIFeedbackItem> feedback = new ArrayList<>();
        AIFeedbackItem item1 = new AIFeedbackItem();
        item1.setId(UUID.randomUUID().toString());
        item1.setLineNumber(12);
        item1.setSeverity("warning");
        item1.setTitle("Use logger instead of System.out");
        item1.setMessage("Using `System.out.println` is not recommended for production code. Use a proper logging framework.");
        item1.setSuggestion("Replace with `logger.info(\"Processing data\")`");
        item1.setStatus("open");
        feedback.add(item1);

        AIFeedbackItem item2 = new AIFeedbackItem();
        item2.setId(UUID.randomUUID().toString());
        item2.setLineNumber(15);
        item2.setSeverity("info");
        item2.setTitle("Input validation added");
        item2.setMessage("Good practice to validate input before processing.");
        item2.setSuggestion("Consider adding more specific validation rules");
        item2.setStatus("open");
        feedback.add(item2);

        response.setFeedback(feedback);

        // Highlighted lines
        List<HighlightedLine> highlightedLines = new ArrayList<>();
        highlightedLines.add(new HighlightedLine(12, "warning", "Use logger instead of System.out"));
        highlightedLines.add(new HighlightedLine(15, "info", "Input validation added"));
        response.setHighlightedLines(highlightedLines);

        return response;
    }

    @Transactional(readOnly = true)
    public List<AIFeedbackItem> getAIFeedback(Long projectId) {
        ComparisonResponse response = getComparisonData(projectId);
        return response.getFeedback();
    }

    @Transactional(readOnly = true)
    public JiraRequirement getJiraRequirement(String requirementId) {
        JiraRequirement requirement = new JiraRequirement();
        requirement.setId(requirementId);
        requirement.setKey("REQ-" + requirementId);
        requirement.setSummary("Sample Requirement");
        requirement.setDescription("Sample requirement description");
        requirement.setAcceptanceCriteria(List.of("Criterion 1", "Criterion 2"));
        requirement.setPriority("Medium");
        requirement.setStatus("Open");
        return requirement;
    }

    @Transactional(readOnly = true)
    public String getGitHubDiff(Long projectId, String branch, String baseBranch, String filePath) {
        // Placeholder diff - in production this would fetch from GitHub API
        return """
diff --git a/sample.java b/sample.java
index 1234567..abcdefg 100644
--- a/sample.java
+++ b/sample.java
@@ -1,3 +1,5 @@
 public class Sample {
-    int x;
+    private int x;
+    public int getX() { return x; }
 }
""";
    }

    @Transactional
    public void updateFeedbackStatus(Long projectId, String feedbackId, String status) {
        // In production, this would update the feedback status in the database
        // For now, it's a no-op placeholder
    }
}
