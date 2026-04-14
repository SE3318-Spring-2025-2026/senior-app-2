package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_sprints", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "sprint_no"})
})
public class ProjectSprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "sprint_no", nullable = false)
    private Integer sprintNo;

    @Column(nullable = false)
    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectDeliverable> deliverables = new ArrayList<>();

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectEvaluation> evaluations = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

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

    public List<ProjectDeliverable> getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(List<ProjectDeliverable> deliverables) {
        this.deliverables = deliverables;
    }

    public List<ProjectEvaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<ProjectEvaluation> evaluations) {
        this.evaluations = evaluations;
    }
}
