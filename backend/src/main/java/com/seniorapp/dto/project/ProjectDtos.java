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

    public static class ProjectSummary {
        private Long projectId;
        private Long templateId;
        private String title;
        private String term;
        private String status;
        private Long activeGroupId;
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
    }

    public static class ProjectDetail {
        private Long projectId;
        private Long templateId;
        private String title;
        private String term;
        private String status;
        private Long createdByUserId;
        private Long activeGroupId;
        private List<SprintDto> sprints;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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
        private String type;
        private String title;
        private String description;
        private Integer weight;
        private boolean fileUploadDeliverable;
        private boolean autoAddToAllSprints;
        private List<RubricDto> rubrics;

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
        private String title;
        private Integer weight;
        private boolean autoAddToAllSprints;
        private List<RubricDto> rubrics;

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
        private String title;
        private String criteriaType;

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
}
