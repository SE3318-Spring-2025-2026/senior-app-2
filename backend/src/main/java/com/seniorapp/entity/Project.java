package com.seniorapp.entity;

import jakarta.persistence.*;

/**
 * Definition of a Project
 */
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @Column(unique = true)
    private String projectId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int sprintCount;

    @Column(nullable = false)
    private String deliverables;

    public Project() {}

    public Project(
        String id,
        String name,
        int sprintCount,
        String deliverables
    ) {
        this.projectId = id;
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

    public String getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(String deliverables) {
        this.deliverables = deliverables;
    }
}