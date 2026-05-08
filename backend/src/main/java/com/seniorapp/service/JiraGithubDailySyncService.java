package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectIssueSyncSnapshot;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.User;
import com.seniorapp.repository.ProjectIssueSyncSnapshotRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JiraGithubDailySyncService {
    private static final Logger log = LoggerFactory.getLogger(JiraGithubDailySyncService.class);
    private static final Pattern SPRINT_NO_PATTERN = Pattern.compile(".*?(\\d+).*");
    private static final Pattern LEGACY_SPRINT_NAME_PATTERN = Pattern.compile(".*name=([^,\\]]+).*");

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectIssueSyncSnapshotRepository snapshotRepository;
    private final SecureOutboundApiService secureOutboundApiService;
    private final IntegrationCredentialCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private boolean loggedIssueSample = false;

    @Value("${jira.oauth.client.id:}")
    private String jiraClientId;

    @Value("${jira.oauth.client.secret:}")
    private String jiraClientSecret;

    public JiraGithubDailySyncService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectIssueSyncSnapshotRepository snapshotRepository,
            SecureOutboundApiService secureOutboundApiService,
            IntegrationCredentialCryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.secureOutboundApiService = secureOutboundApiService;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void syncDaily() {
        for (Project project : projectRepository.findAll()) {
            if (project.getJiraProjectKey() == null || project.getRepoFullName() == null) continue;
            try {
                syncProject(project);
            } catch (Exception ex) {
                log.warn("Daily Jira/GitHub sync failed for project {}: {}", project.getId(), ex.getMessage());
            }
        }
    }

    public void syncProjectNow(Long projectId) {
        if (projectId == null) return;
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null || project.getJiraProjectKey() == null || project.getRepoFullName() == null) {
            return;
        }
        try {
            syncProject(project);
        } catch (Exception ex) {
            log.warn("On-demand Jira/GitHub sync failed for project {}: {}", projectId, ex.getMessage());
        }
    }

    private void syncProject(Project project) throws Exception {
        ProjectTemplate template = project.getTemplate();
        Long creatorUserId = template != null ? template.getCreatedByUserId() : null;
        if (creatorUserId == null) return;
        User creator = userRepository.findById(creatorUserId).orElse(null);
        if (creator == null || creator.getJiraApiTokenEncrypted() == null
                || creator.getGithubPatEncrypted() == null) {
            return;
        }

        String jiraSite = resolveTemplateJiraSiteUrl(project);
        if (jiraSite == null || jiraSite.isBlank()) {
            return;
        }
        String jiraApiBaseUrl;
        try {
            jiraApiBaseUrl = resolveJiraApiBaseUrl(jiraSite, creator.getJiraApiTokenEncrypted());
        } catch (Exception ex) {
            if (!isUnauthorized(ex) || !refreshJiraAccessToken(creator)) {
                throw ex;
            }
            jiraApiBaseUrl = resolveJiraApiBaseUrl(jiraSite, creator.getJiraApiTokenEncrypted());
        }
        List<IssueWithSprintNo> issues = collectIssuesForProject(project, jiraApiBaseUrl, creator.getJiraApiTokenEncrypted());
        if (issues.isEmpty()) return;

        List<String> branchNames = fetchGithubBranches(project.getRepoFullName(), creator.getGithubPatEncrypted());
        String owner = project.getRepoFullName().contains("/") ? project.getRepoFullName().split("/")[0] : null;

        for (IssueWithSprintNo issueItem : issues) {
            JsonNode issue = issueItem.issue();
            if (!loggedIssueSample) {
                log.info("Jira raw issue sample (projectId={}): {}", project.getId(), issue.toString());
                loggedIssueSample = true;
            }
            String issueKey = issue.path("key").asText(null);
            if (issueKey == null || issueKey.isBlank()) continue;
            String matchedBranch = branchNames.stream()
                    .filter(b -> b.toUpperCase().startsWith(issueKey.toUpperCase()))
                    .findFirst().orElse(null);
            Integer prNumber = null;
            Boolean prMerged = null;
            if (matchedBranch != null && owner != null) {
                JsonNode pr = fetchRelatedPr(project.getRepoFullName(), owner, matchedBranch, creator.getGithubPatEncrypted());
                if (pr != null) {
                    prNumber = pr.path("number").isNumber() ? pr.path("number").asInt() : null;
                    prMerged = !pr.path("merged_at").isNull();
                }
            }

            ProjectIssueSyncSnapshot snapshot = snapshotRepository
                    .findByProject_IdAndIssueKey(project.getId(), issueKey)
                    .orElseGet(ProjectIssueSyncSnapshot::new);
            snapshot.setProject(project);
            snapshot.setIssueKey(issueKey);
            String issueTitle = issue.path("fields").path("summary").asText(null);
            snapshot.setIssueTitle(issueTitle);
            snapshot.setSprintNo(issueItem.sprintNo());
            snapshot.setWorkType(issue.path("fields").path("issuetype").path("name").asText(null));
            snapshot.setAssignee(issue.path("fields").path("assignee").path("displayName").asText(null));
            snapshot.setReporter(issue.path("fields").path("reporter").path("displayName").asText(null));
            snapshot.setResolution(issue.path("fields").path("resolution").path("name").asText(null));
            snapshot.setCreatedRemote(parseOffsetDateTime(issue.path("fields").path("created").asText(null)));
            snapshot.setUpdatedRemote(parseOffsetDateTime(issue.path("fields").path("updated").asText(null)));
            JsonNode descNode = issue.path("fields").path("description");
            snapshot.setIssueDescription((descNode.isMissingNode() || descNode.isNull()) ? null : descNode.toString());
            snapshot.setStoryPoints(extractStoryPoints(issue));
            snapshot.setBranchName(matchedBranch);
            snapshot.setPrNumber(prNumber);
            snapshot.setPrMerged(prMerged);
            snapshot.setSyncedAt(LocalDateTime.now());
            snapshotRepository.save(snapshot);
        }
    }

    private List<IssueWithSprintNo> collectIssuesForProject(Project project, String jiraApiBaseUrl, String encryptedApiToken) throws Exception {
        String boardId = project.getJiraBoardId();
        if (boardId == null || boardId.isBlank()) {
            boardId = resolveBoardIdByProjectKey(project.getJiraProjectKey(), jiraApiBaseUrl, encryptedApiToken);
        }
        if (boardId != null && !boardId.isBlank()) {
            List<IssueWithSprintNo> byBoard = collectIssuesByBoardSprints(boardId, jiraApiBaseUrl, encryptedApiToken);
            if (!byBoard.isEmpty()) return byBoard;
        }
        return collectIssuesBySearch(project, jiraApiBaseUrl, encryptedApiToken);
    }

    private List<IssueWithSprintNo> collectIssuesByBoardSprints(String boardId, String jiraApiBaseUrl, String encryptedApiToken) throws Exception {
        String sprintEndpoint = jiraApiBaseUrl + "/rest/agile/1.0/board/" + boardId + "/sprint?state=active,closed,future&maxResults=100";
        JsonNode sprintResponse = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                encryptedApiToken, sprintEndpoint, null, HttpMethod.GET).getBody());
        JsonNode sprintValues = sprintResponse.path("values");
        if (!sprintValues.isArray() || sprintValues.isEmpty()) {
            return List.of();
        }

        List<IssueWithSprintNo> result = new ArrayList<>();
        for (JsonNode sprintNode : sprintValues) {
            String sprintId = sprintNode.path("id").asText(null);
            Integer sprintNo = parseSprintNo(sprintNode.path("name").asText(null));
            if (sprintId == null || sprintId.isBlank()) {
                continue;
            }
            String issuesEndpoint = jiraApiBaseUrl + "/rest/agile/1.0/sprint/" + sprintId
                    + "/issue?maxResults=100&fields=issuetype,summary,assignee,reporter,resolution,created,updated,description,customfield_10016,customfield_10026";
            JsonNode issuesResponse = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, issuesEndpoint, null, HttpMethod.GET).getBody());
            JsonNode issues = issuesResponse.path("issues");
            if (!issues.isArray()) {
                continue;
            }
            for (JsonNode issueNode : issues) {
                result.add(new IssueWithSprintNo(issueNode, sprintNo));
            }
        }
        return result;
    }

    private String resolveBoardIdByProjectKey(String projectKey, String jiraApiBaseUrl, String encryptedApiToken) {
        try {
            if (projectKey == null || projectKey.isBlank()) {
                return null;
            }
            String endpoint = jiraApiBaseUrl + "/rest/agile/1.0/board?projectKeyOrId=" + projectKey + "&type=scrum";
            JsonNode boardResponse = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, endpoint, null, HttpMethod.GET).getBody());
            JsonNode values = boardResponse.path("values");
            if (values.isArray() && values.size() > 0) {
                return values.get(0).path("id").asText(null);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private List<IssueWithSprintNo> collectIssuesBySearch(Project project, String jiraApiBaseUrl, String encryptedApiToken) throws Exception {
        String jiraJql = "project = " + project.getJiraProjectKey() + " ORDER BY updated DESC";
        String jiraEndpoint = jiraApiBaseUrl + "/rest/api/3/search/jql";
        var jiraSearchBody = objectMapper.createObjectNode()
                .put("jql", jiraJql);
        jiraSearchBody.putArray("fields")
                .add("*all");
        JsonNode jiraResponse = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                encryptedApiToken, jiraEndpoint, jiraSearchBody.toString(), HttpMethod.POST).getBody());
        JsonNode issues = jiraResponse.path("issues");
        if (!issues.isArray()) {
            return List.of();
        }
        List<IssueWithSprintNo> result = new ArrayList<>();
        for (JsonNode issueNode : issues) {
            Integer sprintNo = extractSprintNoFromFields(issueNode.path("fields"));
            result.add(new IssueWithSprintNo(issueNode, sprintNo));
        }
        return result;
    }

    private List<String> fetchGithubBranches(String repoFullName, String encryptedPat) throws Exception {
        List<String> branches = new ArrayList<>();
        int page = 1;
        while (true) {
            String endpoint = "https://api.github.com/repos/" + repoFullName + "/branches?per_page=100&page=" + page;
            JsonNode response = objectMapper.readTree(secureOutboundApiService.executeGitHubApiCall(
                    encryptedPat, endpoint, null, HttpMethod.GET).getBody());
            if (!response.isArray() || response.isEmpty()) {
                break;
            }
            for (JsonNode node : response) {
                String name = node.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    branches.add(name);
                }
            }
            if (response.size() < 100) {
                break;
            }
            page++;
        }
        return branches;
    }

    private Integer extractSprintNoFromFields(JsonNode fieldsNode) {
        if (fieldsNode == null || fieldsNode.isMissingNode() || fieldsNode.isNull()) {
            return null;
        }
        Integer sprintNo = parseSprintNo(fieldsNode.path("sprint").path("name").asText(null));
        if (sprintNo != null) {
            return sprintNo;
        }

        Integer bestSprintNo = null;
        if (fieldsNode.has("customfield_10020")) {
            JsonNode legacySprintNode = fieldsNode.path("customfield_10020");
            if (legacySprintNode.isArray()) {
                for (JsonNode sprintNode : legacySprintNode) {
                    Integer parsed = parseSprintNoFromLegacyNode(sprintNode);
                    if (parsed != null && (bestSprintNo == null || parsed > bestSprintNo)) {
                        bestSprintNo = parsed;
                    }
                }
            } else {
                bestSprintNo = parseSprintNoFromLegacyNode(legacySprintNode);
            }
        }
        return bestSprintNo;
    }

    private Integer parseSprintNoFromLegacyNode(JsonNode sprintNode) {
        if (sprintNode == null || sprintNode.isNull() || sprintNode.isMissingNode()) {
            return null;
        }
        if (sprintNode.isObject()) {
            return parseSprintNo(sprintNode.path("name").asText(null));
        }
        if (!sprintNode.isTextual()) {
            return null;
        }
        String raw = sprintNode.asText();
        Matcher legacyMatcher = LEGACY_SPRINT_NAME_PATTERN.matcher(raw);
        if (!legacyMatcher.matches()) {
            return null;
        }
        return parseSprintNo(legacyMatcher.group(1));
    }

    private Integer parseSprintNo(String sprintName) {
        if (sprintName == null || sprintName.isBlank()) {
            return null;
        }
        Matcher matcher = SPRINT_NO_PATTERN.matcher(sprintName);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double extractStoryPoints(JsonNode issueNode) {
        if (issueNode == null || issueNode.isMissingNode() || issueNode.isNull()) {
            return null;
        }
        Double agileEstimate = nodeToDouble(issueNode.path("estimateStatistic").path("statFieldValue").path("value"));
        if (agileEstimate != null) {
            return agileEstimate;
        }
        agileEstimate = nodeToDouble(issueNode.path("estimateStatistic").path("value").path("value"));
        if (agileEstimate != null) {
            return agileEstimate;
        }
        return extractStoryPointsFromFields(issueNode.path("fields"));
    }

    private Double extractStoryPointsFromFields(JsonNode fieldsNode) {
        if (fieldsNode == null || fieldsNode.isMissingNode() || fieldsNode.isNull()) {
            return null;
        }
        // Common Jira story point field ids across workspace types/tenants.
        String[] candidateFieldIds = {
                "customfield_10016",
                "customfield_10026",
                "customfield_10024",
                "customfield_10034",
                "customfield_10002"
        };
        for (String fieldId : candidateFieldIds) {
            Double v = nodeToDouble(fieldsNode.path(fieldId));
            if (v != null) {
                return v;
            }
        }

        // Tenant-specific Story Points field ids can vary; pick numeric custom field as fallback.
        var fields = fieldsNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String key = entry.getKey();
            if (key == null || !key.startsWith("customfield_")) {
                continue;
            }
            Double candidate = nodeToDouble(entry.getValue());
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Double nodeToDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText().trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        if (node.isObject()) {
            Double nested = nodeToDouble(node.path("value"));
            if (nested != null) {
                return nested;
            }
            nested = nodeToDouble(node.path("estimate"));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private record IssueWithSprintNo(JsonNode issue, Integer sprintNo) {}

    private JsonNode fetchRelatedPr(String repoFullName, String owner, String branchName, String encryptedPat) throws Exception {
        String endpoint = "https://api.github.com/repos/" + repoFullName + "/pulls?state=all&head="
                + URLEncoder.encode(owner + ":" + branchName, StandardCharsets.UTF_8);
        JsonNode response = objectMapper.readTree(secureOutboundApiService.executeGitHubApiCall(
                encryptedPat, endpoint, null, HttpMethod.GET).getBody());
        if (response.isArray() && response.size() > 0) {
            return response.get(0);
        }
        return null;
    }

    private String normalizeSiteUrl(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String resolveJiraApiBaseUrl(String siteUrl, String encryptedApiToken) throws Exception {
        String endpoint = "https://api.atlassian.com/oauth/token/accessible-resources";
        JsonNode resources = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                encryptedApiToken, endpoint, null, HttpMethod.GET).getBody());
        if (!resources.isArray() || resources.isEmpty()) {
            throw new IllegalStateException("No Jira cloud resources are accessible for this OAuth token.");
        }

        String targetHost = extractHost(siteUrl);
        String cloudId = null;
        for (JsonNode resource : resources) {
            String resourceUrl = resource.path("url").asText(null);
            if (resourceUrl == null || resourceUrl.isBlank()) {
                continue;
            }
            String resourceHost = extractHost(resourceUrl);
            if (targetHost != null && targetHost.equalsIgnoreCase(resourceHost)) {
                cloudId = resource.path("id").asText(null);
                break;
            }
        }
        if (cloudId == null || cloudId.isBlank()) {
            cloudId = resources.get(0).path("id").asText(null);
        }
        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalStateException("Jira cloudId could not be resolved from accessible resources.");
        }
        return "https://api.atlassian.com/ex/jira/" + cloudId;
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return URI.create(normalizeSiteUrl(url)).getHost();
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveTemplateJiraSiteUrl(Project project) {
        try {
            String raw = project.getTemplate() != null ? project.getTemplate().getTemplateJson() : null;
            if (raw == null || raw.isBlank()) return null;
            return normalizeSiteUrl(objectMapper.readTree(raw).path("jiraSiteUrl").asText(null));
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isUnauthorized(Exception ex) {
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase().contains("unauthorized");
    }

    private boolean refreshJiraAccessToken(User user) {
        try {
            if (user == null || user.getJiraRefreshTokenEncrypted() == null || user.getJiraRefreshTokenEncrypted().isBlank()) {
                return false;
            }
            String refreshToken = cryptoService.decrypt(user.getJiraRefreshTokenEncrypted());
            if (refreshToken == null || refreshToken.isBlank() || jiraClientId == null || jiraClientId.isBlank()
                    || jiraClientSecret == null || jiraClientSecret.isBlank()) {
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> tokenBody = Map.of(
                    "grant_type", "refresh_token",
                    "client_id", jiraClientId,
                    "client_secret", jiraClientSecret,
                    "refresh_token", refreshToken
            );
            ResponseEntity<Map<String, Object>> tokenRes = restTemplate.exchange(
                    "https://auth.atlassian.com/oauth/token",
                    HttpMethod.POST,
                    new HttpEntity<>(tokenBody, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> tokenMap = tokenRes.getBody();
            if (tokenMap == null || !(tokenMap.get("access_token") instanceof String accessToken)
                    || accessToken.isBlank()) {
                return false;
            }

            user.setJiraApiTokenEncrypted(cryptoService.encrypt(accessToken));
            if (tokenMap.get("refresh_token") instanceof String newRefresh && !newRefresh.isBlank()) {
                user.setJiraRefreshTokenEncrypted(cryptoService.encrypt(newRefresh));
            }
            userRepository.save(user);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
