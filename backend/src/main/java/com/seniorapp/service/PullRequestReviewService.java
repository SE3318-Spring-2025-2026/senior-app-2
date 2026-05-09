package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.comparison.ComparisonDtos;
import com.seniorapp.dto.project.PullRequestReviewDtos;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectIssueSyncSnapshot;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.User;
import com.seniorapp.repository.ProjectIssueSyncSnapshotRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.service.ai.TaskCodeAlignmentValidatorService;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class PullRequestReviewService {
    private final ProjectRepository projectRepository;
    private final ProjectIssueSyncSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final SecureOutboundApiService secureOutboundApiService;
    private final ObjectMapper objectMapper;
    private final TaskCodeAlignmentValidatorService taskCodeAlignmentValidatorService;

    public PullRequestReviewService(
            ProjectRepository projectRepository,
            ProjectIssueSyncSnapshotRepository snapshotRepository,
            UserRepository userRepository,
            SecureOutboundApiService secureOutboundApiService,
            ObjectMapper objectMapper,
            TaskCodeAlignmentValidatorService taskCodeAlignmentValidatorService
    ) {
        this.projectRepository = projectRepository;
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
        this.secureOutboundApiService = secureOutboundApiService;
        this.objectMapper = objectMapper;
        this.taskCodeAlignmentValidatorService = taskCodeAlignmentValidatorService;
    }

    @Transactional(readOnly = true)
    public PullRequestReviewDtos.PullRequestReviewResponse getReview(
            Long projectId,
            String issueKey,
            Integer prNumber
    ) throws Exception {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required");
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + safeProjectId));

        ProjectIssueSyncSnapshot snapshot = resolveSnapshot(safeProjectId, issueKey, prNumber);
        Integer resolvedPrNumber = snapshot.getPrNumber() != null ? snapshot.getPrNumber() : prNumber;
        if (resolvedPrNumber == null) {
            throw new IllegalArgumentException("PR number is required.");
        }

        String encryptedPat = resolveTemplateCreatorPat(project);
        if (encryptedPat == null || encryptedPat.isBlank()) {
            throw new IllegalArgumentException("GitHub integration PAT is missing on template creator profile.");
        }
        String repo = project.getRepoFullName();
        if (repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("Project GitHub repo is not configured.");
        }

        JsonNode pr = readJson(githubCall(encryptedPat, "https://api.github.com/repos/" + repo + "/pulls/" + resolvedPrNumber));
        JsonNode files = readJson(githubCall(encryptedPat, "https://api.github.com/repos/" + repo + "/pulls/" + resolvedPrNumber + "/files?per_page=100"));
        JsonNode reviewComments = readJson(githubCall(encryptedPat, "https://api.github.com/repos/" + repo + "/pulls/" + resolvedPrNumber + "/comments?per_page=100"));
        JsonNode reviews = readJson(githubCall(encryptedPat, "https://api.github.com/repos/" + repo + "/pulls/" + resolvedPrNumber + "/reviews?per_page=100"));

        String diff = buildDiff(files);
        String reviewText = buildReviewText(reviewComments, reviews);
        String issueTitle = firstNonBlank(snapshot.getIssueTitle(), snapshot.getIssueKey());
        String issueDescription = firstNonBlank(snapshot.getIssueDescription(), "");
        String prTitle = pr.path("title").asText("PR #" + resolvedPrNumber);

        ComparisonDtos.AIFeedbackItemList reviewProcess = taskCodeAlignmentValidatorService.validate(
                "Pull Request Review Process",
                "Check that review conversation happened and includes meaningful feedback.",
                List.of(
                        "At least one human review exists",
                        "Review comments discuss code quality, correctness, or architecture",
                        "Review state is not only pending without feedback"
                ),
                "PR: " + prTitle + "\n\nReviews and comments:\n" + reviewText
        );

        ComparisonDtos.AIFeedbackItemList implementationCheck = taskCodeAlignmentValidatorService.validate(
                issueTitle,
                issueDescription,
                List.of("Implementation should align with issue description and acceptance intent."),
                diff
        );

        PullRequestReviewDtos.PullRequestReviewResponse out = new PullRequestReviewDtos.PullRequestReviewResponse();
        out.setProjectId(safeProjectId);
        out.setPrNumber(resolvedPrNumber);
        out.setPrTitle(prTitle);
        out.setPrUrl(pr.path("html_url").asText(null));
        out.setBaseBranch(pr.path("base").path("ref").asText(null));
        out.setHeadBranch(pr.path("head").path("ref").asText(null));
        out.setDiff(diff);

        PullRequestReviewDtos.IssueInfo issue = new PullRequestReviewDtos.IssueInfo();
        issue.setKey(snapshot.getIssueKey());
        issue.setTitle(snapshot.getIssueTitle());
        issue.setDescription(snapshot.getIssueDescription());
        issue.setAssignee(snapshot.getAssignee());
        issue.setStoryPoints(snapshot.getStoryPoints());
        out.setIssue(issue);

        out.setReviewProcessAi(PullRequestReviewDtos.AiWidget.from(
                "AI to Read Pull Requests (Difficulty Level 3)",
                reviewProcess
        ));
        out.setImplementationAi(PullRequestReviewDtos.AiWidget.from(
                "AI to Validate Issue Implementation (Difficulty Level 4)",
                implementationCheck
        ));
        return out;
    }

    private ProjectIssueSyncSnapshot resolveSnapshot(Long projectId, String issueKey, Integer prNumber) {
        if (issueKey != null && !issueKey.isBlank()) {
            return snapshotRepository.findByProject_IdAndIssueKey(projectId, issueKey.trim())
                    .orElseThrow(() -> new NoSuchElementException("Issue snapshot not found: " + issueKey));
        }
        if (prNumber != null) {
            return snapshotRepository.findByProject_IdAndPrNumber(projectId, prNumber)
                    .orElseThrow(() -> new NoSuchElementException("PR snapshot not found: " + prNumber));
        }
        throw new IllegalArgumentException("Either issueKey or prNumber is required.");
    }

    private String githubCall(String encryptedPat, String endpoint) {
        return secureOutboundApiService.executeGitHubApiCall(encryptedPat, endpoint, null, HttpMethod.GET).getBody();
    }

    private JsonNode readJson(String body) throws Exception {
        return objectMapper.readTree(body != null ? body : "{}");
    }

    private String buildDiff(JsonNode files) {
        if (!files.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode f : files) {
            String fileName = f.path("filename").asText("");
            String patch = f.path("patch").asText("");
            if (fileName.isBlank()) continue;
            sb.append("diff -- ").append(fileName).append("\n");
            if (!patch.isBlank()) {
                sb.append(patch).append("\n\n");
            }
        }
        return sb.toString();
    }

    private String buildReviewText(JsonNode comments, JsonNode reviews) {
        List<String> lines = new ArrayList<>();
        if (reviews.isArray()) {
            for (JsonNode r : reviews) {
                String user = r.path("user").path("login").asText("unknown");
                String state = r.path("state").asText("UNKNOWN").toUpperCase(Locale.ROOT);
                String body = r.path("body").asText("");
                lines.add("REVIEW by " + user + " [" + state + "]: " + body);
            }
        }
        if (comments.isArray()) {
            for (JsonNode c : comments) {
                String user = c.path("user").path("login").asText("unknown");
                String body = c.path("body").asText("");
                String path = c.path("path").asText("");
                lines.add("COMMENT by " + user + " (" + path + "): " + body);
            }
        }
        return String.join("\n", lines);
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }

    private String resolveTemplateCreatorPat(Project project) {
        ProjectTemplate template = project.getTemplate();
        if (template == null || template.getCreatedByUserId() == null) return null;
        Long creatorUserId = Objects.requireNonNull(template.getCreatedByUserId(), "template createdByUserId required");
        User creator = userRepository.findById(creatorUserId).orElse(null);
        if (creator == null) return null;
        String pat = creator.getGithubPatEncrypted();
        return (pat != null && !pat.isBlank()) ? pat : null;
    }
}
