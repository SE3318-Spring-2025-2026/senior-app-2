package com.seniorapp.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_deliverables")
public class ProjectDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_sprint_id", nullable = false)
    private ProjectSprint sprint;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private boolean fileUploadDeliverable;

    @Column(nullable = false)
    private boolean autoAddToAllSprints;

    @OneToMany(mappedBy = "deliverable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectDeliverableRubric> rubrics = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectSprint getSprint() {
        return sprint;
    }

    public void setSprint(ProjectSprint sprint) {
        this.sprint = sprint;
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

    public List<ProjectDeliverableRubric> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<ProjectDeliverableRubric> rubrics) {
        this.rubrics = rubrics;
    }
}
