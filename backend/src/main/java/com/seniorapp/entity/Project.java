package com.seniorapp.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Definition of a Project
 */
@Entity
@Table(name = "projects")
@Data
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

}