package com.seniorapp.dto;

import java.util.List;

public class ProjectTemplateResponse {
    private List<ProjectTemplateItem> projects;

    public ProjectTemplateResponse(List<ProjectTemplateItem> projects) {
        this.projects = projects;
    }

    public List<ProjectTemplateItem> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectTemplateItem> projects) {
        this.projects = projects;
    }

    public static class ProjectTemplateItem {
    private String projectId;
    private String name;
    private int sprintCount;
    private List<String> deliverables;

    public ProjectTemplateItem(String projectId, String name, int sprintCount, List<String> deliverables) {
        this.projectId = projectId;
        this.name = name;
        this.sprintCount = sprintCount;
        this.deliverables = deliverables;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSprintCount() {
        return sprintCount;
    }

    public void setSprintCount(int sprintCount) {
        this.sprintCount = sprintCount;
    }

    public List<String> getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(List<String> deliverables) {
        this.deliverables = deliverables;
    }
    }
}
