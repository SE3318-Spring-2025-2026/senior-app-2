package com.seniorapp.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public static class CreateProjectRequest {
        private Long templateId;
        private String title;
        private String term;
        private Long groupId;

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }
    }

    public static class AssignGroupRequest {
        private Long groupId;

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }
    }

    public static class CreateCommitteeRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class AddProfessorToCommitteeRequest {
        private Long professorUserId;

        public Long getProfessorUserId() {
            return professorUserId;
        }

        public void setProfessorUserId(Long professorUserId) {
            this.professorUserId = professorUserId;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdResponse {
        private String status;
        private Data data;

        public IdResponse(String status, Long projectId) {
            this.status = status;
            this.data = new Data(projectId);
        }

        public String getStatus() {
            return status;
        }

        public Data getData() {
            return data;
        }

        public static class Data {
            private Long projectId;

            public Data(Long projectId) {
                this.projectId = projectId;
            }

            public Long getProjectId() {
                return projectId;
            }
        }
    }

    public static class AssignmentResponse {
        private String status;
        private Long projectId;
        private Long groupId;
        private LocalDateTime assignedAt;

        public AssignmentResponse(String status, Long projectId, Long groupId, LocalDateTime assignedAt) {
            this.status = status;
            this.projectId = projectId;
            this.groupId = groupId;
            this.assignedAt = assignedAt;
        }

        public String getStatus() {
            return status;
        }

        public Long getProjectId() {
            return projectId;
        }

        public Long getGroupId() {
            return groupId;
        }

        public LocalDateTime getAssignedAt() {
            return assignedAt;
        }
    }

    public static class ProjectListResponse {
        private String status;
        private List<ProjectSummary> data;

        public ProjectListResponse(String status, List<ProjectSummary> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public List<ProjectSummary> getData() {
            return data;
        }
    }

    public static class ProjectDetailResponse {
        private String status;
        private ProjectDetail data;

        public ProjectDetailResponse(String status, ProjectDetail data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public ProjectDetail getData() {
            return data;
        }
    }

    public static class CommitteeListResponse {
        private String status;
        private List<CommitteeDto> data;

        public CommitteeListResponse(String status, List<CommitteeDto> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public List<CommitteeDto> getData() {
            return data;
        }
    }

    public static class ProfessorListResponse {
        private String status;
        private List<ProfessorOptionDto> data;

        public ProfessorListResponse(String status, List<ProfessorOptionDto> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public List<ProfessorOptionDto> getData() {
            return data;
        }
    }

    public static class ProjectSummary {
        private Long projectId;
        private Long templateId;
        private String title;
        private String term;
        private String status;
        private Long activeGroupId;
        private String repoFullName;
        private String repoHtmlUrl;
        private String repoDefaultBranch;
        private String repoProviderId;
        private LocalDateTime createdAt;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getActiveGroupId() {
            return activeGroupId;
        }

        public void setActiveGroupId(Long activeGroupId) {
            this.activeGroupId = activeGroupId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getRepoFullName() {
            return repoFullName;
        }

        public void setRepoFullName(String repoFullName) {
            this.repoFullName = repoFullName;
        }

        public String getRepoHtmlUrl() {
            return repoHtmlUrl;
        }

        public void setRepoHtmlUrl(String repoHtmlUrl) {
            this.repoHtmlUrl = repoHtmlUrl;
        }

        public String getRepoDefaultBranch() {
            return repoDefaultBranch;
        }

        public void setRepoDefaultBranch(String repoDefaultBranch) {
            this.repoDefaultBranch = repoDefaultBranch;
        }

        public String getRepoProviderId() {
            return repoProviderId;
        }

        public void setRepoProviderId(String repoProviderId) {
            this.repoProviderId = repoProviderId;
        }
    }

    public static class CommitteeDto {
        private Long committeeId;
        private Long projectId;
        private String name;
        private List<ProfessorOptionDto> professors;

        public Long getCommitteeId() {
            return committeeId;
        }

        public void setCommitteeId(Long committeeId) {
            this.committeeId = committeeId;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<ProfessorOptionDto> getProfessors() {
            return professors;
        }

        public void setProfessors(List<ProfessorOptionDto> professors) {
            this.professors = professors;
        }
    }

    public static class ProfessorOptionDto {
        private Long userId;
        private String fullName;
        private String email;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ProjectDetail {
        private Long projectId;
        private Long templateId;
        private String title;
        private String term;
        private String status;
        private Long createdByUserId;
        private Long activeGroupId;
        private String repoFullName;
        private String repoHtmlUrl;
        private String repoDefaultBranch;
        private String repoProviderId;
        private String jiraProjectKey;
        private String jiraProjectUrl;
        private List<GithubIssueDto> githubIssues;
        private List<String> githubBranches;
        private List<JiraGithubMatchDto> jiraGithubMatches;
        private StoryPointValidationDto storyPointValidation;
        private List<SprintDto> sprints;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** Server-computed PDF-style grades for {@link #activeGroupId}; null when no group on project. */
        private ProjectGradingSummaryDto gradingSummary;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getCreatedByUserId() {
            return createdByUserId;
        }

        public void setCreatedByUserId(Long createdByUserId) {
            this.createdByUserId = createdByUserId;
        }

        public Long getActiveGroupId() {
            return activeGroupId;
        }

        public void setActiveGroupId(Long activeGroupId) {
            this.activeGroupId = activeGroupId;
        }

        public String getRepoFullName() {
            return repoFullName;
        }

        public void setRepoFullName(String repoFullName) {
            this.repoFullName = repoFullName;
        }

        public String getRepoHtmlUrl() {
            return repoHtmlUrl;
        }

        public void setRepoHtmlUrl(String repoHtmlUrl) {
            this.repoHtmlUrl = repoHtmlUrl;
        }

        public String getRepoDefaultBranch() {
            return repoDefaultBranch;
        }

        public void setRepoDefaultBranch(String repoDefaultBranch) {
            this.repoDefaultBranch = repoDefaultBranch;
        }

        public String getRepoProviderId() {
            return repoProviderId;
        }

        public void setRepoProviderId(String repoProviderId) {
            this.repoProviderId = repoProviderId;
        }

        public String getJiraProjectKey() {
            return jiraProjectKey;
        }

        public void setJiraProjectKey(String jiraProjectKey) {
            this.jiraProjectKey = jiraProjectKey;
        }

        public String getJiraProjectUrl() {
            return jiraProjectUrl;
        }

        public void setJiraProjectUrl(String jiraProjectUrl) {
            this.jiraProjectUrl = jiraProjectUrl;
        }

        public List<GithubIssueDto> getGithubIssues() {
            return githubIssues;
        }

        public void setGithubIssues(List<GithubIssueDto> githubIssues) {
            this.githubIssues = githubIssues;
        }

        public List<String> getGithubBranches() {
            return githubBranches;
        }

        public void setGithubBranches(List<String> githubBranches) {
            this.githubBranches = githubBranches;
        }

        public List<JiraGithubMatchDto> getJiraGithubMatches() {
            return jiraGithubMatches;
        }

        public void setJiraGithubMatches(List<JiraGithubMatchDto> jiraGithubMatches) {
            this.jiraGithubMatches = jiraGithubMatches;
        }

        public StoryPointValidationDto getStoryPointValidation() {
            return storyPointValidation;
        }

        public void setStoryPointValidation(StoryPointValidationDto storyPointValidation) {
            this.storyPointValidation = storyPointValidation;
        }

        public List<SprintDto> getSprints() {
            return sprints;
        }

        public void setSprints(List<SprintDto> sprints) {
            this.sprints = sprints;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public ProjectGradingSummaryDto getGradingSummary() {
            return gradingSummary;
        }

        public void setGradingSummary(ProjectGradingSummaryDto gradingSummary) {
            this.gradingSummary = gradingSummary;
        }
    }

    public static class SprintDto {
        private Integer sprintNo;
        private String title;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<DeliverableDto> deliverables;
        private List<EvaluationDto> evaluations;

        public Integer getSprintNo() {
            return sprintNo;
        }

        public void setSprintNo(Integer sprintNo) {
            this.sprintNo = sprintNo;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public List<DeliverableDto> getDeliverables() {
            return deliverables;
        }

        public void setDeliverables(List<DeliverableDto> deliverables) {
            this.deliverables = deliverables;
        }

        public List<EvaluationDto> getEvaluations() {
            return evaluations;
        }

        public void setEvaluations(List<EvaluationDto> evaluations) {
            this.evaluations = evaluations;
        }
    }

    public static class DeliverableDto {
        private Long id;
        private String type;
        private String title;
        private String description;
        private Integer weight;
        private boolean fileUploadDeliverable;
        private boolean autoAddToAllSprints;
        private List<RubricDto> rubrics;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        public boolean isFileUploadDeliverable() {
            return fileUploadDeliverable;
        }

        public void setFileUploadDeliverable(boolean fileUploadDeliverable) {
            this.fileUploadDeliverable = fileUploadDeliverable;
        }

        public boolean isAutoAddToAllSprints() {
            return autoAddToAllSprints;
        }

        public void setAutoAddToAllSprints(boolean autoAddToAllSprints) {
            this.autoAddToAllSprints = autoAddToAllSprints;
        }

        public List<RubricDto> getRubrics() {
            return rubrics;
        }

        public void setRubrics(List<RubricDto> rubrics) {
            this.rubrics = rubrics;
        }
    }

    public static class EvaluationDto {
        private Long id;
        private String title;
        private Integer weight;
        private boolean autoAddToAllSprints;
        private List<RubricDto> rubrics;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        public boolean isAutoAddToAllSprints() {
            return autoAddToAllSprints;
        }

        public void setAutoAddToAllSprints(boolean autoAddToAllSprints) {
            this.autoAddToAllSprints = autoAddToAllSprints;
        }

        public List<RubricDto> getRubrics() {
            return rubrics;
        }

        public void setRubrics(List<RubricDto> rubrics) {
            this.rubrics = rubrics;
        }
    }

    public static class RubricDto {
        /** project_deliverable_rubrics / project_evaluation_rubrics birincil anahtarı. */
        private Long id;
        private String title;
        private String criteriaType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCriteriaType() {
            return criteriaType;
        }

        public void setCriteriaType(String criteriaType) {
            this.criteriaType = criteriaType;
        }
    }

    public static class GithubIssueDto {
        private Long issueNumber;
        private String title;
        private String state;
        private String assignee;
        private Integer sprintNo;
        private Double storyPoints;

        public Long getIssueNumber() { return issueNumber; }
        public void setIssueNumber(Long issueNumber) { this.issueNumber = issueNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public Integer getSprintNo() { return sprintNo; }
        public void setSprintNo(Integer sprintNo) { this.sprintNo = sprintNo; }
        public Double getStoryPoints() { return storyPoints; }
        public void setStoryPoints(Double storyPoints) { this.storyPoints = storyPoints; }
    }

    public static class StoryPointValidationDto {
        private boolean matched;
        private Double expected;
        private Double actual;

        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        public Double getExpected() { return expected; }
        public void setExpected(Double expected) { this.expected = expected; }
        public Double getActual() { return actual; }
        public void setActual(Double actual) { this.actual = actual; }
    }

    public static class JiraGithubMatchDto {
        private String branchName;
        private String issueKey;
        private String issueTitle;
        private String issueDescription;
        private Double storyPoints;
        private Integer sprintNo;
        private String jiraAssignee;
        private Boolean prMerged;

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getIssueKey() {
            return issueKey;
        }

        public void setIssueKey(String issueKey) {
            this.issueKey = issueKey;
        }

        public String getIssueTitle() {
            return issueTitle;
        }

        public void setIssueTitle(String issueTitle) {
            this.issueTitle = issueTitle;
        }

        public String getIssueDescription() {
            return issueDescription;
        }

        public void setIssueDescription(String issueDescription) {
            this.issueDescription = issueDescription;
        }

        public Double getStoryPoints() {
            return storyPoints;
        }

        public void setStoryPoints(Double storyPoints) {
            this.storyPoints = storyPoints;
        }

        public Integer getSprintNo() {
            return sprintNo;
        }

        public void setSprintNo(Integer sprintNo) {
            this.sprintNo = sprintNo;
        }

        public String getJiraAssignee() {
            return jiraAssignee;
        }

        public void setJiraAssignee(String jiraAssignee) {
            this.jiraAssignee = jiraAssignee;
        }

        public Boolean getPrMerged() {
            return prMerged;
        }

        public void setPrMerged(Boolean prMerged) {
            this.prMerged = prMerged;
        }
    }

    public static class AdvisorLiveGradesResponse {
        private String status;
        private List<AdvisorLiveGradeRow> data;

        public AdvisorLiveGradesResponse(String status, List<AdvisorLiveGradeRow> data) {
            this.status = status;
            this.data = data;
        }
        public String getStatus() { return status; }
        public List<AdvisorLiveGradeRow> getData() { return data; }
    }

    public static class AdvisorLiveGradeRow {
        private Long projectId;
        private String projectTitle;
        private Long groupId;
        private Double cumulativeTeamGrade;
        private Double adjustedIndividualGrade;
        private Double overallSuccessGrade;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectTitle() { return projectTitle; }
        public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public Double getCumulativeTeamGrade() { return cumulativeTeamGrade; }
        public void setCumulativeTeamGrade(Double cumulativeTeamGrade) { this.cumulativeTeamGrade = cumulativeTeamGrade; }
        public Double getAdjustedIndividualGrade() { return adjustedIndividualGrade; }
        public void setAdjustedIndividualGrade(Double adjustedIndividualGrade) { this.adjustedIndividualGrade = adjustedIndividualGrade; }
        public Double getOverallSuccessGrade() { return overallSuccessGrade; }
        public void setOverallSuccessGrade(Double overallSuccessGrade) { this.overallSuccessGrade = overallSuccessGrade; }
    }
}
