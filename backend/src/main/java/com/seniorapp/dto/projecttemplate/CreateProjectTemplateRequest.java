package com.seniorapp.dto.projecttemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProjectTemplateRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotBlank
    private String term;

    @NotBlank
    private String projectStartDate;

    @NotNull
    private JsonNode sprints;
    private Boolean createGithubRepo;
    private Boolean createJiraWorkspace;
    private String jiraSiteUrl;

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

    public JsonNode getSprints() {
        return sprints;
    }

    public void setSprints(JsonNode sprints) {
        this.sprints = sprints;
    }

    public String getProjectStartDate() {
        return projectStartDate;
    }

    public void setProjectStartDate(String projectStartDate) {
        this.projectStartDate = projectStartDate;
    }

    public Boolean getCreateGithubRepo() {
        return createGithubRepo;
    }

    public void setCreateGithubRepo(Boolean createGithubRepo) {
        this.createGithubRepo = createGithubRepo;
    }

    public Boolean getCreateJiraWorkspace() {
        return createJiraWorkspace;
    }

    public void setCreateJiraWorkspace(Boolean createJiraWorkspace) {
        this.createJiraWorkspace = createJiraWorkspace;
    }

    public String getJiraSiteUrl() {
        return jiraSiteUrl;
    }

    public void setJiraSiteUrl(String jiraSiteUrl) {
        this.jiraSiteUrl = jiraSiteUrl;
    }
}
