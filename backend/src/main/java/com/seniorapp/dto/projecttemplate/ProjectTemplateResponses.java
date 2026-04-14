package com.seniorapp.dto.projecttemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ProjectTemplateResponses {

    private ProjectTemplateResponses() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TemplateCreatedResponse {
        private String status;
        private String message;
        private Data data;

        public TemplateCreatedResponse(String status, String message, Long templateId) {
            this.status = status;
            this.message = message;
            this.data = new Data(templateId);
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Data getData() {
            return data;
        }

        public static class Data {
            private Long templateId;

            public Data(Long templateId) {
                this.templateId = templateId;
            }

            public Long getTemplateId() {
                return templateId;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectTemplateSummary {
        private Long templateId;
        private String name;
        private String description;
        private String term;
        private Long createdByUserId;
        private LocalDate projectStartDate;
        private Integer version;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Long getCreatedByUserId() {
            return createdByUserId;
        }

        public void setCreatedByUserId(Long createdByUserId) {
            this.createdByUserId = createdByUserId;
        }

        public LocalDate getProjectStartDate() {
            return projectStartDate;
        }

        public void setProjectStartDate(LocalDate projectStartDate) {
            this.projectStartDate = projectStartDate;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectTemplateListResponse {
        private String status;
        private List<ProjectTemplateSummary> data;

        public ProjectTemplateListResponse(String status, List<ProjectTemplateSummary> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public List<ProjectTemplateSummary> getData() {
            return data;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectTemplateDetail {
        private Long templateId;
        private String name;
        private String description;
        private String term;
        private Long createdByUserId;
        private LocalDate projectStartDate;
        private Integer version;
        private boolean active;
        private JsonNode payload;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Long getCreatedByUserId() {
            return createdByUserId;
        }

        public void setCreatedByUserId(Long createdByUserId) {
            this.createdByUserId = createdByUserId;
        }

        public LocalDate getProjectStartDate() {
            return projectStartDate;
        }

        public void setProjectStartDate(LocalDate projectStartDate) {
            this.projectStartDate = projectStartDate;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public JsonNode getPayload() {
            return payload;
        }

        public void setPayload(JsonNode payload) {
            this.payload = payload;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectTemplateDetailResponse {
        private String status;
        private ProjectTemplateDetail data;

        public ProjectTemplateDetailResponse(String status, ProjectTemplateDetail data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public ProjectTemplateDetail getData() {
            return data;
        }
    }
}
