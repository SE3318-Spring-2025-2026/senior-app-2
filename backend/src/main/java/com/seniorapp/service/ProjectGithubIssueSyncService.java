package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectGithubIssue;
import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.ProjectStudentStoryPoint;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.ProjectGithubIssueRepository;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.ProjectStudentStoryPointRepository;
import com.seniorapp.repository.UserGroupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGithubIssueSyncService {
    private final ProjectSprintRepository sprintRepository;
    private final UserGroupRepository userGroupRepository;
    private final ProjectGithubIssueRepository issueRepository;
    private final ProjectStudentStoryPointRepository storyPointRepository;
    private final SecureOutboundApiService secureOutboundApiService;
    private final ObjectMapper objectMapper;

    public ProjectGithubIssueSyncService(
            ProjectSprintRepository sprintRepository,
            UserGroupRepository userGroupRepository,
            ProjectGithubIssueRepository issueRepository,
            ProjectStudentStoryPointRepository storyPointRepository,
            SecureOutboundApiService secureOutboundApiService,
            ObjectMapper objectMapper) {
        this.sprintRepository = sprintRepository;
        this.userGroupRepository = userGroupRepository;
        this.issueRepository = issueRepository;
        this.storyPointRepository = storyPointRepository;
        this.secureOutboundApiService = secureOutboundApiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncEndedSprints() {
        LocalDate today = LocalDate.now();
        List<ProjectSprint> ended = sprintRepository.findByEndDateLessThanEqual(today);
        for (ProjectSprint sprint : ended) {
            Project project = sprint.getProject();
            if (project == null || project.getGroupId() == null || project.getRepoFullName() == null) {
                continue;
            }
            Long groupId = project.getGroupId();
            if (groupId == null) continue;
            UserGroup group = userGroupRepository.findById(groupId).orElse(null);
            if (group == null) {
                continue;
            }
            String encryptedPat = null;
            if (group.getCoordinator() != null) {
                encryptedPat = group.getCoordinator().getGithubPatEncrypted();
            }
            if (encryptedPat == null || encryptedPat.isBlank()) {
                encryptedPat = group.getGithubPatEncrypted();
            }
            if (encryptedPat == null || encryptedPat.isBlank()) {
                continue;
            }
            String endpoint = "https://api.github.com/repos/" + project.getRepoFullName() + "/issues?state=open&per_page=100";
            try {
                String body = secureOutboundApiService
                        .executeGitHubApiCall(encryptedPat, endpoint, null, HttpMethod.GET)
                        .getBody();
                JsonNode arr = objectMapper.readTree(body);
                if (!arr.isArray()) continue;
                for (JsonNode issueNode : arr) {
                    if (issueNode.hasNonNull("pull_request")) continue;
                    Long issueNumber = issueNode.path("number").asLong();
                    ProjectGithubIssue row = issueRepository
                            .findByProject_IdAndIssueNumber(project.getId(), issueNumber)
                            .orElseGet(ProjectGithubIssue::new);
                    row.setProject(project);
                    row.setSprint(sprint);
                    row.setIssueNumber(issueNumber);
                    row.setTitle(issueNode.path("title").asText(""));
                    row.setState(issueNode.path("state").asText("open").toLowerCase(Locale.ROOT));
                    row.setAssignee(issueNode.path("assignee").path("login").asText(null));
                    row.setStoryPoints(extractStoryPoints(issueNode));
                    issueRepository.save(row);
                }
            } catch (Exception ignored) {
                // Best-effort background sync; API errors should not break scheduler loop.
            }
        }
    }

    @Transactional(readOnly = true)
    public StoryPointValidationResult validateStoryPoints(Long projectId) {
        List<ProjectStudentStoryPoint> manual = storyPointRepository.findByProject_Id(projectId);
        double expected = manual.stream().map(ProjectStudentStoryPoint::getStoryPoints).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        List<ProjectGithubIssue> issues = issueRepository.findByProject_IdOrderByIssueNumberAsc(projectId);
        double actual = issues.stream().map(ProjectGithubIssue::getStoryPoints).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        boolean matched = Math.abs(expected - actual) < 0.0001;
        return new StoryPointValidationResult(matched, expected, actual, issues);
    }

    private Double extractStoryPoints(JsonNode issueNode) {
        JsonNode labels = issueNode.path("labels");
        if (!labels.isArray()) return null;
        for (JsonNode label : labels) {
            String name = label.path("name").asText("");
            if (!name.toLowerCase(Locale.ROOT).startsWith("sp:")) continue;
            try {
                return Double.parseDouble(name.substring(3).trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    public record StoryPointValidationResult(
            boolean matched, Double expected, Double actual, List<ProjectGithubIssue> issues) {}
}
