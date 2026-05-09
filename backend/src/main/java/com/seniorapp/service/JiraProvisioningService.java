package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class JiraProvisioningService {
    private final SecureOutboundApiService secureOutboundApiService;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final IntegrationCredentialCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.oauth.client.id:}")
    private String jiraClientId;

    @Value("${jira.oauth.client.secret:}")
    private String jiraClientSecret;

    public JiraProvisioningService(
            SecureOutboundApiService secureOutboundApiService,
            UserGroupMemberRepository userGroupMemberRepository,
            UserGroupRepository userGroupRepository,
            UserRepository userRepository,
            IntegrationCredentialCryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.secureOutboundApiService = secureOutboundApiService;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    public JiraProvisioningResult provisionProject(Project project, Long groupId, String jiraSiteUrlInput, User tokenOwner) throws Exception {
        if (tokenOwner == null || tokenOwner.getJiraApiTokenEncrypted() == null || tokenOwner.getJiraApiTokenEncrypted().isBlank()) {
            throw new IllegalArgumentException("Template Jira OAuth connection is missing.");
        }
        String encryptedApiToken = tokenOwner.getJiraApiTokenEncrypted();
        try {
            return runProvisioning(project, groupId, jiraSiteUrlInput, encryptedApiToken);
        } catch (Exception ex) {
            if (!isUnauthorized(ex) || !refreshJiraAccessToken(tokenOwner)) {
                throw ex;
            }
            return runProvisioning(project, groupId, jiraSiteUrlInput, tokenOwner.getJiraApiTokenEncrypted());
        }
    }

    private JiraProvisioningResult runProvisioning(Project project, Long groupId, String jiraSiteUrlInput, String encryptedApiToken) throws Exception {
        String jiraSiteUrl = normalizeSiteUrl(jiraSiteUrlInput);
        String jiraApiBaseUrl = resolveJiraApiBaseUrl(jiraSiteUrl, encryptedApiToken);
        String projectKey = buildProjectKey(project);
        String projectLeadAccountId = resolveProjectLeadAccountId(groupId, jiraApiBaseUrl, encryptedApiToken);
        JsonNode projectNode = createJiraProject(project, jiraApiBaseUrl, encryptedApiToken, projectKey, projectLeadAccountId);
        String jiraProjectId = projectNode.path("id").asText(null);
        String jiraProjectKey = projectNode.path("key").asText(projectKey);

        String jiraBoardId = ensureBoardAndCreateSprints(project, jiraApiBaseUrl, encryptedApiToken, jiraProjectKey);
        assignStudentsToJiraProject(groupId, jiraApiBaseUrl, encryptedApiToken, jiraProjectKey);

        JiraProvisioningResult result = new JiraProvisioningResult();
        result.setProjectId(jiraProjectId);
        result.setProjectKey(jiraProjectKey);
        result.setBoardId(jiraBoardId);
        return result;
    }

    private boolean isUnauthorized(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("unauthorized") || lower.contains("\"code\":401");
    }

    private boolean refreshJiraAccessToken(User user) {
        try {
            if (user == null || user.getJiraRefreshTokenEncrypted() == null || user.getJiraRefreshTokenEncrypted().isBlank()) {
                return false;
            }
            String refreshToken = cryptoService.decrypt(user.getJiraRefreshTokenEncrypted());
            if (refreshToken == null || refreshToken.isBlank()
                    || jiraClientId == null || jiraClientId.isBlank()
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
                    Objects.requireNonNull(HttpMethod.POST),
                    new HttpEntity<>(tokenBody, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> tokenMap = tokenRes.getBody();
            if (tokenMap == null || !(tokenMap.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
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

    private JsonNode createJiraProject(Project project, String siteUrl, String encryptedApiToken,
                                       String projectKey, String projectLeadAccountId) throws Exception {
        String endpoint = siteUrl + "/rest/api/3/project";
        var bodyNode = objectMapper.createObjectNode()
                .put("key", projectKey)
                .put("name", project.getTitle())
                .put("projectTypeKey", "software")
                .put("projectTemplateKey", "com.pyxis.greenhopper.jira:gh-simplified-scrum-classic")
                .put("description", "SeniorApp project " + project.getId());
        if (projectLeadAccountId != null && !projectLeadAccountId.isBlank()) {
            bodyNode.put("leadAccountId", projectLeadAccountId);
        }
        String body = bodyNode.toString();
        String response = secureOutboundApiService.executeJiraApiCall(
                encryptedApiToken, endpoint, body, HttpMethod.POST).getBody();
        return objectMapper.readTree(response);
    }

    private String ensureBoardAndCreateSprints(Project project, String siteUrl, String encryptedApiToken, String jiraProjectKey) {
        String boardId = null;
        try {
            String boardEndpoint = siteUrl + "/rest/agile/1.0/board?projectKeyOrId=" + jiraProjectKey + "&type=scrum";
            JsonNode boardResponse = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, boardEndpoint, null, HttpMethod.GET).getBody());
            JsonNode values = boardResponse.path("values");
            if (values.isArray() && values.size() > 0) {
                boardId = values.get(0).path("id").asText(null);
            }
        } catch (Exception ex) {
            // Best effort: project should still be provisioned even if board/sprint access fails.
            return null;
        }

        if (boardId == null || boardId.isBlank()) {
            return null;
        }

        List<ProjectSprint> plannedSprints = project.getSprints().stream()
                .sorted(Comparator.comparing(ProjectSprint::getSprintNo))
                .toList();
        List<Long> existingSprintIds = fetchBoardSprintIds(siteUrl, encryptedApiToken, boardId);

        int index = 0;
        for (ProjectSprint sprint : plannedSprints) {
            String sprintName = "Sprint " + sprint.getSprintNo();
            try {
                if (index < existingSprintIds.size()) {
                    updateSprint(siteUrl, encryptedApiToken, existingSprintIds.get(index), sprintName, sprint);
                } else {
                    createSprint(siteUrl, encryptedApiToken, boardId, sprintName, sprint);
                }
            } catch (Exception ex) {
                // best effort sprint creation/update
            }
            index++;
        }
        return boardId;
    }

    private List<Long> fetchBoardSprintIds(String siteUrl, String encryptedApiToken, String boardId) {
        try {
            String endpoint = siteUrl + "/rest/agile/1.0/board/" + boardId + "/sprint?maxResults=50&state=future,active";
            JsonNode response = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, endpoint, null, HttpMethod.GET).getBody());
            JsonNode values = response.path("values");
            if (!values.isArray()) {
                return List.of();
            }
            return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                    .map(node -> node.path("id").asLong(0))
                    .filter(id -> id > 0)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void createSprint(String siteUrl, String encryptedApiToken, String boardId,
                              String sprintName, ProjectSprint sprint) {
        String sprintEndpoint = siteUrl + "/rest/agile/1.0/sprint";
        var bodyNode = objectMapper.createObjectNode()
                .put("name", sprintName)
                .put("originBoardId", Long.parseLong(boardId));
        String startDate = toJiraDateTime(sprint.getStartDate(), false);
        String endDate = toJiraDateTime(sprint.getEndDate(), true);
        if (startDate != null) bodyNode.put("startDate", startDate);
        if (endDate != null) bodyNode.put("endDate", endDate);
        secureOutboundApiService.executeJiraApiCall(encryptedApiToken, sprintEndpoint, bodyNode.toString(), HttpMethod.POST);
    }

    private void updateSprint(String siteUrl, String encryptedApiToken, Long sprintId,
                              String sprintName, ProjectSprint sprint) {
        String sprintEndpoint = siteUrl + "/rest/agile/1.0/sprint/" + sprintId;
        var bodyNode = objectMapper.createObjectNode()
                .put("name", sprintName);
        String startDate = toJiraDateTime(sprint.getStartDate(), false);
        String endDate = toJiraDateTime(sprint.getEndDate(), true);
        if (startDate != null) bodyNode.put("startDate", startDate);
        if (endDate != null) bodyNode.put("endDate", endDate);
        secureOutboundApiService.executeJiraApiCall(encryptedApiToken, sprintEndpoint, bodyNode.toString(), HttpMethod.PUT);
    }

    private void assignStudentsToJiraProject(Long groupId, String siteUrl, String encryptedApiToken, String jiraProjectKey) {
        try {
            String roleEndpoint = siteUrl + "/rest/api/3/project/" + jiraProjectKey + "/role";
            JsonNode roleMap = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, roleEndpoint, null, HttpMethod.GET).getBody());
            String targetRoleUrl = null;
            if (roleMap.has("Developers")) {
                targetRoleUrl = roleMap.path("Developers").asText(null);
            }
            if (targetRoleUrl == null || targetRoleUrl.isBlank()) {
                var fields = roleMap.fields();
                if (fields.hasNext()) {
                    targetRoleUrl = fields.next().getValue().asText(null);
                }
            }
            if (targetRoleUrl == null || targetRoleUrl.isBlank()) {
                return;
            }

            List<UserGroupMember> acceptedMembers =
                    userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
            for (UserGroupMember membership : acceptedMembers) {
                User user = membership.getUser();
                if (user == null || user.getRole() != Role.STUDENT) continue;
                if (user.getJiraAccountId() == null || user.getJiraAccountId().isBlank()) continue;
                String body = objectMapper.createObjectNode()
                        .putArray("user").add(user.getJiraAccountId().trim())
                        .toString();
                try {
                    secureOutboundApiService.executeJiraApiCall(
                            encryptedApiToken, targetRoleUrl, body, HttpMethod.POST);
                } catch (Exception ignored) {
                    // best effort student assignment
                }
            }
        } catch (Exception ignored) {
            // best effort overall
        }
    }

    private String normalizeSiteUrl(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
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

    private String resolveProjectLeadAccountId(Long groupId, String jiraApiBaseUrl, String encryptedApiToken) {
        // Prefer the OAuth token owner first; this account is guaranteed to match the active Jira site context.
        String tokenOwnerAccountId = resolveTokenOwnerAccountId(jiraApiBaseUrl, encryptedApiToken);
        if (tokenOwnerAccountId != null && !tokenOwnerAccountId.isBlank()) {
            return tokenOwnerAccountId;
        }

        if (groupId == null) {
            return null;
        }
        return userGroupRepository.findById(groupId)
                .map(group -> {
                    User teamLeader = group.getTeamLeader();
                    if (teamLeader != null && teamLeader.getJiraAccountId() != null
                            && !teamLeader.getJiraAccountId().isBlank()) {
                        return teamLeader.getJiraAccountId().trim();
                    }
                    User coordinator = group.getCoordinator();
                    if (coordinator != null && coordinator.getJiraAccountId() != null
                            && !coordinator.getJiraAccountId().isBlank()) {
                        return coordinator.getJiraAccountId().trim();
                    }
                    return null;
                })
                .orElse(null);
    }

    private String resolveTokenOwnerAccountId(String jiraApiBaseUrl, String encryptedApiToken) {
        try {
            String endpoint = jiraApiBaseUrl + "/rest/api/3/myself";
            JsonNode me = objectMapper.readTree(secureOutboundApiService.executeJiraApiCall(
                    encryptedApiToken, endpoint, null, HttpMethod.GET).getBody());
            String accountId = me.path("accountId").asText(null);
            if (accountId != null && !accountId.isBlank()) {
                return accountId.trim();
            }
        } catch (Exception ignored) {
            // Fall back to group leader/coordinator resolution.
        }
        return null;
    }

    private String buildProjectKey(Project project) {
        String base = (project.getTitle() == null ? "SENIOR" : project.getTitle())
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        if (base.isBlank()) base = "SENIOR";
        String key = base.length() > 7 ? base.substring(0, 7) : base;
        return key + project.getId();
    }

    private String toJiraDateTime(LocalDate date, boolean endOfDay) {
        if (date == null) {
            return null;
        }
        LocalDateTime dt = LocalDateTime.of(date, endOfDay ? LocalTime.of(23, 59) : LocalTime.of(9, 0));
        return dt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static class JiraProvisioningResult {
        private String projectKey;
        private String projectId;
        private String boardId;

        public String getProjectKey() {
            return projectKey;
        }

        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getBoardId() {
            return boardId;
        }

        public void setBoardId(String boardId) {
            this.boardId = boardId;
        }
    }
}
